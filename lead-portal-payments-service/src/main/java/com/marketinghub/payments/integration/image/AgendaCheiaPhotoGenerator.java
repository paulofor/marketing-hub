package com.marketinghub.payments.integration.image;

import java.awt.image.BufferedImage;

/** Gera fotografias premium de unhas para a composição dos kits Agenda Cheia. */
public interface AgendaCheiaPhotoGenerator {
    /** Gera uma fotografia sem texto para a variação comercial solicitada. */
    BufferedImage generate(String executionId, int variant);
}
