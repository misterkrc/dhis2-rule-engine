package org.hisp.dhis.rules.functions;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.collect.Sets;
import org.hisp.dhis.rules.RuleVariableValue;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Returns standard deviation based on age, gender and weight
 *
 * @Author Zubair Asghar.
 */
public class RuleFunctionZScore extends RuleFunction
{
    private static final Map<ZScoreTableKey, Map<Float, Integer>> ZSCORE_TABLE_GIRL = ZScoreTable.getZscoreTableGirl();
    private static final Map<ZScoreTableKey, Map<Float, Integer>> ZSCORE_TABLE_BOY = ZScoreTable.getZscoreTableBoy();

    private static final Set<String> GENDER_CODES = Sets.newHashSet( "male", "MALE", "Male","ma", "m", "M", "0", "false" );

    public static final String D2_ZSCORE = "d2:zScore";

    @Nonnull
    @Override
    public String evaluate( @Nonnull List<String> arguments, Map<String, RuleVariableValue> valueMap, Map<String,
        List<String>> supplementaryData )
    {
        if ( arguments.size() < 3 )
        {
            throw new IllegalArgumentException( "At least three arguments required but found: " + arguments.size() );
        }

        // 1 = female, 0 = male
        byte age;
        float weight;
        byte gender = GENDER_CODES.contains( arguments.get( 3 ) ) ? (byte) 0 : (byte) 1;

        String zScore = "";

        try
        {
            age = Byte.parseByte( arguments.get( 0 ) );
            weight = Byte.parseByte( arguments.get( 1 ) );
        }
        catch ( NumberFormatException ex )
        {
            throw new IllegalArgumentException( "Byte parsing failed" );
        }

        zScore = getZScore( age, weight, gender );


        return zScore;
    }

    private String getZScore( byte age, float weight, byte gender )
    {
        ZScoreTableKey key = new ZScoreTableKey( (byte) 1, age );

        Map<Float, Integer> sdMap = new HashMap<>();

        // Female
        if ( gender == 1 )
        {
            sdMap = ZSCORE_TABLE_GIRL.get( key );

        }
        else
        {
            sdMap = ZSCORE_TABLE_BOY.get( key );
        }

        int multiplicationFactor = getMultiplicationFactor( sdMap, weight );

        // weight exactly matches with any of the SD values
        if ( sdMap.keySet().contains( weight ) )
        {
            int sd = sdMap.get( weight );

            return String.valueOf( sd * multiplicationFactor );
        }

        float lowerLimitX = 0, higherLimitY = 0;

        for ( float f : sortKeySet( sdMap ) )
        {
            if (  weight > f )
            {
                lowerLimitX = f;
                continue;
            }

            higherLimitY = f;
        }

        float distance = lowerLimitX - higherLimitY;

        float gap = lowerLimitX - weight;

        float decimalAddition = gap / distance;

        float result = sdMap.get( lowerLimitX ) + decimalAddition;

        return String.valueOf( result * multiplicationFactor );
    }

    private int getMultiplicationFactor( Map<Float, Integer> sdMap, float weight )
    {
        float median = findMedian( sdMap );

        return Float.compare( weight, median );
    }

    private float findMedian( Map<Float, Integer> sdMap )
    {
        Float[] array = sortKeySet( sdMap );

        return array[3];
    }

    private Float[] sortKeySet( Map<Float, Integer> sdMap )
    {
        Set<Float> keySet = sdMap.keySet();

        Float[] array = (Float[]) keySet.toArray();

        Arrays.sort( array );

        return array;
    }
}