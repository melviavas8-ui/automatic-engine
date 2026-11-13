package daamky.ua.voice;

import java.util.Arrays;

final class VoiceFeatureExtractor {
   static final int SAMPLE_RATE = 16000;
   private static final int FRAME_SIZE = 400;
   private static final int FRAME_STEP = 160;
   private static final int FFT_SIZE = 512;
   private static final int MEL_FILTERS = 24;
   private static final int CEPSTRAL_COEFFICIENTS = 13;
   private static final float[][] MEL_BANK = createMelBank();

   private VoiceFeatureExtractor() {
   }

   static float[][] extract(byte[] pcm) {
      float[] samples = decodeAndTrim(pcm);
      if (samples.length < 400) {
         return new float[0][0];
      } else {
         for (int i = samples.length - 1; i > 0; i--) {
            samples[i] -= 0.97F * samples[i - 1];
         }

         int frames = 1 + (samples.length - 400) / 160;
         float[][] mfcc = new float[frames][13];
         double[] real = new double[512];
         double[] imaginary = new double[512];
         double[] power = new double[257];
         double[] mel = new double[24];

         for (int frame = 0; frame < frames; frame++) {
            Arrays.fill(real, 0.0);
            Arrays.fill(imaginary, 0.0);
            int offset = frame * 160;

            for (int i = 0; i < 400; i++) {
               double window = 0.54 - 0.46 * Math.cos((Math.PI * 2) * (double)i / 399.0);
               real[i] = (double)samples[offset + i] * window;
            }

            fft(real, imaginary);

            for (int bin = 0; bin < power.length; bin++) {
               power[bin] = (real[bin] * real[bin] + imaginary[bin] * imaginary[bin]) / 512.0;
            }

            for (int filter = 0; filter < 24; filter++) {
               double energy = 1.0E-10;

               for (int bin = 0; bin < power.length; bin++) {
                  energy += power[bin] * (double)MEL_BANK[filter][bin];
               }

               mel[filter] = Math.log(energy);
            }

            for (int coefficient = 0; coefficient < 13; coefficient++) {
               double value = 0.0;

               for (int filter = 0; filter < 24; filter++) {
                  value += mel[filter] * Math.cos(Math.PI * (double)coefficient * ((double)filter + 0.5) / 24.0);
               }

               mfcc[frame][coefficient] = (float)value;
            }
         }

         normalize(mfcc);
         return withDelta(mfcc);
      }
   }

   static double distance(float[][] first, float[][] second) {
      if (first.length != 0 && second.length != 0) {
         int rows = first.length;
         int columns = second.length;
         int band = Math.max(Math.abs(rows - columns) + 3, Math.max(rows, columns) / 3);
         double[][] cost = new double[rows + 1][columns + 1];
         int[][] length = new int[rows + 1][columns + 1];

         for (double[] row : cost) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
         }

         cost[0][0] = 0.0;

         for (int i = 1; i <= rows; i++) {
            int from = Math.max(1, i - band);
            int to = Math.min(columns, i + band);

            for (int j = from; j <= to; j++) {
               double local = frameDistance(first[i - 1], second[j - 1]);
               int previousI = i - 1;
               int previousJ = j - 1;
               double best = cost[previousI][previousJ];
               if (cost[i - 1][j] < best) {
                  best = cost[i - 1][j];
                  previousI = i - 1;
                  previousJ = j;
               }

               if (cost[i][j - 1] < best) {
                  best = cost[i][j - 1];
                  previousI = i;
                  previousJ = j - 1;
               }

               cost[i][j] = local + best;
               length[i][j] = length[previousI][previousJ] + 1;
            }
         }

         if (!Double.isFinite(cost[rows][columns])) {
            return Double.POSITIVE_INFINITY;
         } else {
            double score = cost[rows][columns] / (double)Math.max(1, length[rows][columns]);
            double durationPenalty = Math.abs(Math.log((double)rows / (double)columns)) * 0.42;
            return score + durationPenalty;
         }
      } else {
         return Double.POSITIVE_INFINITY;
      }
   }

   static VoiceFeatureExtractor.SubsequenceMatch subsequence(float[][] template, float[][] input) {
      if (template.length != 0 && input.length != 0) {
         int minimumLength = Math.max(8, Math.round((float)template.length * 0.66F));
         int maximumLength = Math.max(minimumLength, Math.round((float)template.length * 1.42F));
         int maximumStart = Math.min(14, Math.max(0, input.length - minimumLength));
         double bestScore = Double.POSITIVE_INFINITY;
         int bestStart = -1;
         int bestEnd = -1;

         for (int start = 0; start <= maximumStart; start++) {
            int available = input.length - start;
            int longest = Math.min(maximumLength, available);

            for (int length = minimumLength; length <= longest; length += 2) {
               int end = start + length;
               double score = distance(template, Arrays.copyOfRange(input, start, end)) + (double)start * 0.006;
               if (score < bestScore) {
                  bestScore = score;
                  bestStart = start;
                  bestEnd = end;
               }
            }
         }

         if (bestEnd < 0) {
            double score = distance(template, input);
            return new VoiceFeatureExtractor.SubsequenceMatch(score, 0, input.length);
         } else {
            return new VoiceFeatureExtractor.SubsequenceMatch(bestScore, bestStart, bestEnd);
         }
      } else {
         return VoiceFeatureExtractor.SubsequenceMatch.NONE;
      }
   }

   static float[][] slice(float[][] values, int from) {
      return values != null && from < values.length ? Arrays.copyOfRange(values, Math.max(0, from), values.length) : new float[0][0];
   }

   static float[][] renormalize(float[][] values) {
      if (values != null && values.length != 0) {
         float[][] staticCoefficients = new float[values.length][13];

         for (int i = 0; i < values.length; i++) {
            System.arraycopy(values[i], 0, staticCoefficients[i], 0, Math.min(13, values[i].length));
         }

         normalize(staticCoefficients);
         return withDelta(staticCoefficients);
      } else {
         return new float[0][0];
      }
   }

   private static float[] decodeAndTrim(byte[] pcm) {
      int sampleCount = pcm == null ? 0 : pcm.length / 2;
      if (sampleCount == 0) {
         return new float[0];
      } else {
         float[] decoded = new float[sampleCount];

         for (int i = 0; i < sampleCount; i++) {
            int low = pcm[i * 2] & 255;
            int high = pcm[i * 2 + 1];
            decoded[i] = (float)((short)(low | high << 8)) / 32768.0F;
         }

         int window = 160;
         float peakRms = 0.0F;
         float[] rms = new float[Math.max(1, (decoded.length + window - 1) / window)];

         for (int block = 0; block < rms.length; block++) {
            int start = block * window;
            int end = Math.min(decoded.length, start + window);
            double sum = 0.0;

            for (int i = start; i < end; i++) {
               sum += (double)(decoded[i] * decoded[i]);
            }

            rms[block] = (float)Math.sqrt(sum / (double)Math.max(1, end - start));
            peakRms = Math.max(peakRms, rms[block]);
         }

         float threshold = Math.max(0.008F, peakRms * 0.14F);
         int first = 0;
         int last = rms.length - 1;

         while (first < rms.length && rms[first] < threshold) {
            first++;
         }

         while (last > first && rms[last] < threshold) {
            last--;
         }

         int padding = 3 * window;
         int from = Math.max(0, first * window - padding);
         int to = Math.min(decoded.length, (last + 1) * window + padding);
         if (to - from < 400) {
            return new float[0];
         } else {
            float[] trimmed = Arrays.copyOfRange(decoded, from, to);
            float peak = 0.0F;

            for (float sample : trimmed) {
               peak = Math.max(peak, Math.abs(sample));
            }

            float gain = peak > 1.0E-4F ? Math.min(8.0F, 0.92F / peak) : 1.0F;

            for (int i = 0; i < trimmed.length; i++) {
               trimmed[i] *= gain;
            }

            return trimmed;
         }
      }
   }

   private static void normalize(float[][] values) {
      if (values.length != 0) {
         int dimensions = values[0].length;

         for (int dimension = 0; dimension < dimensions; dimension++) {
            double mean = 0.0;

            for (float[] value : values) {
               mean += (double)value[dimension];
            }

            mean /= (double)values.length;
            double variance = 0.0;

            for (float[] value : values) {
               double delta = (double)value[dimension] - mean;
               variance += delta * delta;
            }

            double deviation = Math.sqrt(variance / (double)values.length + 1.0E-5);

            for (float[] value : values) {
               value[dimension] = (float)(((double)value[dimension] - mean) / deviation);
            }
         }
      }
   }

   private static float[][] withDelta(float[][] mfcc) {
      float[][] result = new float[mfcc.length][26];

      for (int frame = 0; frame < mfcc.length; frame++) {
         float[] previous = mfcc[Math.max(0, frame - 1)];
         float[] next = mfcc[Math.min(mfcc.length - 1, frame + 1)];

         for (int coefficient = 0; coefficient < 13; coefficient++) {
            result[frame][coefficient] = mfcc[frame][coefficient];
            result[frame][13 + coefficient] = (next[coefficient] - previous[coefficient]) * 0.5F;
         }
      }

      return result;
   }

   private static double frameDistance(float[] first, float[] second) {
      int dimensions = Math.min(first.length, second.length);
      double sum = 0.0;

      for (int i = 0; i < dimensions; i++) {
         double delta = (double)(first[i] - second[i]);
         sum += delta * delta;
      }

      return Math.sqrt(sum / (double)Math.max(1, dimensions));
   }

   private static float[][] createMelBank() {
      int bins = 257;
      float[][] bank = new float[24][bins];
      double lowMel = hzToMel(80.0);
      double highMel = hzToMel(7600.0);
      int[] points = new int[26];

      for (int i = 0; i < points.length; i++) {
         double mel = lowMel + (highMel - lowMel) * (double)i / (double)(points.length - 1);
         points[i] = Math.max(0, Math.min(bins - 1, (int)Math.floor(513.0 * melToHz(mel) / 16000.0)));
      }

      for (int filter = 0; filter < 24; filter++) {
         int left = points[filter];
         int center = Math.max(left + 1, points[filter + 1]);
         int right = Math.max(center + 1, points[filter + 2]);

         for (int bin = left; bin < center && bin < bins; bin++) {
            bank[filter][bin] = (float)(bin - left) / (float)Math.max(1, center - left);
         }

         for (int bin = center; bin <= right && bin < bins; bin++) {
            bank[filter][bin] = (float)(right - bin) / (float)Math.max(1, right - center);
         }
      }

      return bank;
   }

   private static double hzToMel(double hz) {
      return 2595.0 * Math.log10(1.0 + hz / 700.0);
   }

   private static double melToHz(double mel) {
      return 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0);
   }

   private static void fft(double[] real, double[] imaginary) {
      int n = real.length;
      int i = 1;

      for (int j = 0; i < n; i++) {
         int bit;
         for (bit = n >> 1; (j & bit) != 0; bit >>= 1) {
            j ^= bit;
         }

         j ^= bit;
         if (i < j) {
            double realValue = real[i];
            real[i] = real[j];
            real[j] = realValue;
            double imaginaryValue = imaginary[i];
            imaginary[i] = imaginary[j];
            imaginary[j] = imaginaryValue;
         }
      }

      for (int length = 2; length <= n; length <<= 1) {
         double angle = (-Math.PI * 2) / (double)length;
         double rootReal = Math.cos(angle);
         double rootImaginary = Math.sin(angle);

         for (int offset = 0; offset < n; offset += length) {
            double currentReal = 1.0;
            double currentImaginary = 0.0;

            for (int ix = 0; ix < length / 2; ix++) {
               int even = offset + ix;
               int odd = even + length / 2;
               double oddReal = real[odd] * currentReal - imaginary[odd] * currentImaginary;
               double oddImaginary = real[odd] * currentImaginary + imaginary[odd] * currentReal;
               real[odd] = real[even] - oddReal;
               imaginary[odd] = imaginary[even] - oddImaginary;
               real[even] += oddReal;
               imaginary[even] += oddImaginary;
               double nextReal = currentReal * rootReal - currentImaginary * rootImaginary;
               currentImaginary = currentReal * rootImaginary + currentImaginary * rootReal;
               currentReal = nextReal;
            }
         }
      }
   }

   static record SubsequenceMatch(double score, int startFrame, int endFrame) {
      static final VoiceFeatureExtractor.SubsequenceMatch NONE = new VoiceFeatureExtractor.SubsequenceMatch(Double.POSITIVE_INFINITY, -1, -1);
   }
}
