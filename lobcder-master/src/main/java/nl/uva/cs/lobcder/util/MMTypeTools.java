/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.util.*;
import nl.uva.vlet.data.StringUtil;

/**
 *
 * Source: http://code.google.com/p/mimeparse/
 * 
 */
public class MMTypeTools {

    public static String bestMatch(Collection<String> supported, String header) {
        List<ParseResults> parseResults = new LinkedList<ParseResults>();

        List<FitnessAndQuality> weightedMatches = new LinkedList<FitnessAndQuality>();
        for (String r : header.split(",")) {
            parseResults.add(parseMediaRange(r));
        }

        for (String s : supported) {
            FitnessAndQuality fitnessAndQuality = fitnessAndQualityParsed(s,
                    parseResults);
            fitnessAndQuality.mimeType = s;
            weightedMatches.add(fitnessAndQuality);
        }
        Collections.sort(weightedMatches);

        FitnessAndQuality lastOne = weightedMatches.get(weightedMatches.size() - 1);
//        debug("Get back: " + lastOne.quality + " , " + lastOne.mimeType);

        if (lastOne.quality != 0) {
            return lastOne.mimeType;
        }

        return "";
    }

    /**
     * Find the best match for a given mimeType against a list of media_ranges
     * that have already been parsed by MimeParse.parseMediaRange(). Returns a
     * tuple of the fitness value and the value of the 'q' quality parameter of
     * the best match, or (-1, 0) if no match was found. Just as for
     * quality_parsed(), 'parsed_ranges' must be a list of parsed media ranges.
     * 
     * @param mimeType
     * @param parsedRanges
     */
    private static FitnessAndQuality fitnessAndQualityParsed(String mimeType,
            Collection<ParseResults> parsedRanges) {
        int bestFitness = -1;
        float bestFitQ = 0;
        ParseResults target = parseMediaRange(mimeType);

        for (ParseResults range : parsedRanges) {
            if ((target.type.equals(range.type) || range.type.equals("*") || target.type.equals("*"))
                    && (target.subType.equals(range.subType)
                    || range.subType.equals("*") || target.subType.equals("*"))) {
                for (String k : target.params.keySet()) {
                    int paramMatches = 0;
                    if (!k.equals("q") && range.params.containsKey(k)
                            && target.params.get(k).equals(range.params.get(k))) {
                        paramMatches++;
                    }
                    int fitness = (range.type.equals(target.type)) ? 100 : 0;
                    fitness += (range.subType.equals(target.subType)) ? 10 : 0;
                    fitness += paramMatches;
                    if (fitness > bestFitness) {
                        bestFitness = fitness;
                        if (StringUtil.isEmpty(range.params.get("q"))) {
                            bestFitQ = new Float(0);
                        } else {
                            bestFitQ = new Float(range.params.get("q"));//NumberUtils.toFloat(range.params.get("q"), 0);   
                        }
                    }
                }
            }
        }
        return new FitnessAndQuality(bestFitness, bestFitQ);
    }

    /**
     * Carves up a media range and returns a ParseResults.
     * 
     * For example, the media range 'application/*;q=0.5' would get parsed into:
     * 
     * ('application', '*', {'q', '0.5'})
     * 
     * In addition this function also guarantees that there is a value for 'q'
     * in the params dictionary, filling it in with a proper default if
     * necessary.
     * 
     * @param range
     */
    private static ParseResults parseMediaRange(String range) {
        ParseResults results = parseMimeType(range);
        String q = results.params.get("q");
        float f;
        if (StringUtil.isEmpty(q)) {
            f = new Float(1);
        } else {
            f = new Float(q);
        }
        if (StringUtil.isEmpty(q) || f < 0 || f > 1) {
            results.params.put("q", "1");
        }
        return results;
    }

    /**
     * Carves up a mime-type and returns a ParseResults object
     * 
     * For example, the media range 'application/xhtml;q=0.5' would get parsed
     * into:
     * 
     * ('application', 'xhtml', {'q', '0.5'})
     */
    private static ParseResults parseMimeType(String mimeType) {
        String[] parts = mimeType.split(";");
        ParseResults results = new ParseResults();
        results.params = new HashMap<String, String>();

        for (int i = 1; i < parts.length; ++i) {
            String p = parts[i];
            String[] subParts = p.split("=");
            if (subParts.length == 2) {
                results.params.put(subParts[0].trim(), subParts[1].trim());
            }
        }
        String fullType = parts[0].trim();

        // Java URLConnection class sends an Accept header that includes a
        // single "*" - Turn it into a legal wildcard.
        if (fullType.equals("*")) {
            fullType = "*/*";
        }
        String[] types = fullType.split("/");
        results.type = types[0].trim();
        results.subType = types[1].trim();
        return results;
    }

    /**
     * Structure for holding a fitness/quality combo
     */
    private static class FitnessAndQuality implements
            Comparable<FitnessAndQuality> {

        int fitness;
        float quality;
        String mimeType; // optionally used

        public FitnessAndQuality(int fitness, float quality) {
            this.fitness = fitness;
            this.quality = quality;
        }

        @Override
        public int compareTo(FitnessAndQuality o) {
            if (fitness == o.fitness) {
                if (quality == o.quality) {
                    return 0;
                } else {
                    return quality < o.quality ? -1 : 1;
                }
            } else {
                return fitness < o.fitness ? -1 : 1;
            }
        }
    }

    /**
     * Parse results container
     */
    private static class ParseResults {

        String type;
        String subType;
        // !a dictionary of all the parameters for the media range
        Map<String, String> params;

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("('" + type + "', '" + subType
                    + "', {");
            for (String k : params.keySet()) {
                s.append("'" + k + "':'" + params.get(k) + "',");
            }
            return s.append("})").toString();
        }
    }
}
