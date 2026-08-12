package com.marketinghub.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.deliverable.DeliverablePackage;
import com.marketinghub.hypothesis.Hypothesis;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a identidade de experimento contra ciclos no grafo JPA. */
class ExperimentIdentityTest {

  /** Comprova que o hash do experimento não percorre associações bidirecionais. */
  @Test
  void shouldHashExperimentWithoutTraversingAssociatedGraph() {
    Hypothesis hypothesis = new Hypothesis();
    DeliverablePackage deliverablePackage = new DeliverablePackage();
    hypothesis.setOfferPackage(deliverablePackage);
    deliverablePackage.setHypothesis(hypothesis);

    Experiment experiment = new Experiment();
    experiment.setId(85L);
    experiment.setHypothesisRef(hypothesis);

    var experiments = new LinkedHashSet<Experiment>();
    experiments.add(experiment);

    assertThat(experiments).contains(experiment);
  }

  /** Comprova que somente entidades persistidas com o mesmo identificador são iguais. */
  @Test
  void shouldCompareExperimentsByPersistentIdentity() {
    Experiment first = new Experiment();
    Experiment sameIdentity = new Experiment();
    Experiment transientExperiment = new Experiment();
    first.setId(85L);
    sameIdentity.setId(85L);

    assertThat(first).isEqualTo(sameIdentity);
    assertThat(first).isNotEqualTo(transientExperiment);
    assertThat(new Experiment()).isNotEqualTo(transientExperiment);
  }
}
