package com.marketinghub.facebookads.playbook.dto;

/**
 * Request payload to (re)start the workflow for an experiment.
 */
public record StartExperimentAdSetWorkflowRequest(boolean restart) {
}
