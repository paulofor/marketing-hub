# Fonte revisada v1 — biomarcadores não decifram emoções isoladamente

**Revisão:** 2026-10-11. **Referência:** Laeng, B. et al. *Machine learning classification of emotional videos via autonomic arousal based on pupil diameters, electrodermal activity and heart rate*. International Journal of Psychophysiology 228:113455, online 25/08/2026, volume outubro. https://doi.org/10.1016/j.ijpsycho.2026.113455

**Método:** 47 participantes assistiram a dez vídeos curtos e escolheram rótulos (divertimento, assombro, repulsa, amor cuidador ou tristeza). Diâmetro pupilar, EDA e medidas cardíacas alimentaram classificadores SVC, RF e XGBoost; HRV excessivamente ruidosa foi descartada da análise central.

**Achado:** pupila foi uma variável importante, mas assinaturas autonômicas se sobrepuseram: divertimento e tristeza (alta ativação) ou assombro e repulsa (baixa ativação) podiam ser confundidos. Não há um mapa individual universal pupila→prazer.

**Mecanismo proposto:** processos autonômicos compartilhados por estados emocionais diferentes não identificam valência e significado da experiência.

**Força:** média-baixa para generalização individual, com n=47, dez vídeos e rótulos forçados.

**Limites/riscos:** ruído, iluminação, foco do olhar, consentimento biométrico, ausência de validação externa comprovada no resumo. Não mede dopamina nem venda.

**Hipótese testável:** comparar classificação por pupila bruta a abordagem calibrada com luminância, gaze, baseline e autorrelato, validada em pessoas e vídeos não vistos; avaliar erros por classe e privacidade.
