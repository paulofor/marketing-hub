package com.marketinghub.experiment.service;

import org.springframework.stereotype.Service;

/** Calcula o intervalo binomial exato usado para comunicar a incerteza da conversão. */
@Service
public class ExperimentBinomialConfidenceService {

  private static final double TWO_SIDED_TAIL_95 = 0.025d;
  private static final int INVERSE_ITERATIONS = 120;
  private static final int FRACTION_ITERATIONS = 240;
  private static final double FRACTION_EPSILON = 3.0e-14d;
  private static final double FRACTION_MINIMUM = 1.0e-300d;
  private static final double[] LANCZOS_COEFFICIENTS = {
    676.5203681218851d,
    -1259.1392167224028d,
    771.32342877765313d,
    -176.61502916214059d,
    12.507343278686905d,
    -0.13857109526572012d,
    9.9843695780195716e-6d,
    1.5056327351493116e-7d
  };

  /** Devolve os limites bilaterais de Clopper-Pearson para 95% de confiança. */
  public Interval exact95(long trials, long successes) {
    if (trials <= 0 || successes < 0 || successes > trials) {
      throw new IllegalArgumentException("A amostra binomial precisa ter contagens válidas.");
    }
    double lower =
        successes == 0
            ? 0.0d
            : inverseRegularizedBeta(TWO_SIDED_TAIL_95, successes, trials - successes + 1.0d);
    double upper =
        successes == trials
            ? 1.0d
            : inverseRegularizedBeta(
                1.0d - TWO_SIDED_TAIL_95, successes + 1.0d, trials - successes);
    return new Interval(lower, upper);
  }

  /**
   * Localiza o quantil beta por bisseção monotônica sem adicionar dependência estatística externa.
   */
  private double inverseRegularizedBeta(double probability, double alpha, double beta) {
    double lower = 0.0d;
    double upper = 1.0d;
    for (int iteration = 0; iteration < INVERSE_ITERATIONS; iteration++) {
      double middle = (lower + upper) / 2.0d;
      if (regularizedBeta(middle, alpha, beta) < probability) {
        lower = middle;
      } else {
        upper = middle;
      }
    }
    return (lower + upper) / 2.0d;
  }

  /** Avalia a beta incompleta regularizada usando simetria e fração contínua estável. */
  private double regularizedBeta(double value, double alpha, double beta) {
    if (value <= 0.0d) {
      return 0.0d;
    }
    if (value >= 1.0d) {
      return 1.0d;
    }
    double logarithmicFactor =
        logGamma(alpha + beta)
            - logGamma(alpha)
            - logGamma(beta)
            + alpha * Math.log(value)
            + beta * Math.log1p(-value);
    double factor = Math.exp(logarithmicFactor);
    double result =
        value < (alpha + 1.0d) / (alpha + beta + 2.0d)
            ? factor * betaFraction(value, alpha, beta) / alpha
            : 1.0d - factor * betaFraction(1.0d - value, beta, alpha) / beta;
    return Math.max(0.0d, Math.min(1.0d, result));
  }

  /** Resolve a fração contínua da beta incompleta pelo método modificado de Lentz. */
  private double betaFraction(double value, double alpha, double beta) {
    double alphaPlusBeta = alpha + beta;
    double alphaPlusOne = alpha + 1.0d;
    double alphaMinusOne = alpha - 1.0d;
    double denominator = 1.0d - alphaPlusBeta * value / alphaPlusOne;
    if (Math.abs(denominator) < FRACTION_MINIMUM) {
      denominator = FRACTION_MINIMUM;
    }
    denominator = 1.0d / denominator;
    double numerator = 1.0d;
    double result = denominator;
    for (int iteration = 1; iteration <= FRACTION_ITERATIONS; iteration++) {
      int doubled = 2 * iteration;
      double coefficient =
          iteration * (beta - iteration) * value / ((alphaMinusOne + doubled) * (alpha + doubled));
      denominator = stableDenominator(1.0d + coefficient * denominator);
      numerator = stableDenominator(1.0d + coefficient / numerator);
      denominator = 1.0d / denominator;
      result *= denominator * numerator;

      coefficient =
          -(alpha + iteration)
              * (alphaPlusBeta + iteration)
              * value
              / ((alpha + doubled) * (alphaPlusOne + doubled));
      denominator = stableDenominator(1.0d + coefficient * denominator);
      numerator = stableDenominator(1.0d + coefficient / numerator);
      denominator = 1.0d / denominator;
      double delta = denominator * numerator;
      result *= delta;
      if (Math.abs(delta - 1.0d) < FRACTION_EPSILON) {
        return result;
      }
    }
    throw new IllegalStateException("O intervalo binomial exato não convergiu.");
  }

  /** Evita divisão por zero durante a avaliação da fração contínua. */
  private double stableDenominator(double value) {
    if (Math.abs(value) >= FRACTION_MINIMUM) {
      return value;
    }
    return value < 0.0d ? -FRACTION_MINIMUM : FRACTION_MINIMUM;
  }

  /** Calcula o logaritmo da função gama pela aproximação de Lanczos. */
  private double logGamma(double value) {
    if (value < 0.5d) {
      return Math.log(Math.PI) - Math.log(Math.sin(Math.PI * value)) - logGamma(1.0d - value);
    }
    double shifted = value - 1.0d;
    double sum = 0.99999999999980993d;
    for (int index = 0; index < LANCZOS_COEFFICIENTS.length; index++) {
      sum += LANCZOS_COEFFICIENTS[index] / (shifted + index + 1.0d);
    }
    double scale = shifted + LANCZOS_COEFFICIENTS.length - 0.5d;
    return 0.5d * Math.log(2.0d * Math.PI)
        + (shifted + 0.5d) * Math.log(scale)
        - scale
        + Math.log(sum);
  }

  /** Representa os limites inferior e superior como proporções entre zero e um. */
  public record Interval(double lower, double upper) {}
}
