package com.marketinghub.journey.execution;

import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates simple conditions declared in journey steps using SpEL.
 */
@Component
@Slf4j
public class JourneyConditionEvaluator {
    private final ExpressionParser parser = new SpelExpressionParser();

    public boolean evaluateEntryCondition(JourneyAssignment assignment,
                                          JourneyStep step,
                                          Map<String, Object> context) {
        String entryCondition = step.getEntryCondition();
        if (entryCondition == null || entryCondition.isBlank()) {
            return true;
        }
        return evaluate(entryCondition, assignment, step, context);
    }

    public boolean evaluateExitCondition(JourneyAssignment assignment,
                                         JourneyStep step,
                                         Map<String, Object> context) {
        String exitCondition = step.getExitCondition();
        if (exitCondition == null || exitCondition.isBlank()) {
            return false;
        }
        return evaluate(exitCondition, assignment, step, context);
    }

    private boolean evaluate(String expression,
                             JourneyAssignment assignment,
                             JourneyStep step,
                             Map<String, Object> context) {
        try {
            Map<String, Object> root = new HashMap<>();
            root.put("assignment", assignment);
            root.put("journey", assignment.getJourney());
            root.put("lead", assignment.getLead());
            root.put("step", step);
            root.put("context", context);
            SimpleEvaluationContext evalContext = SimpleEvaluationContext
                    .forReadOnlyDataBinding()
                    .withRootObject(root)
                    .build();
            Expression compiled = parser.parseExpression(expression);
            Boolean result = compiled.getValue(evalContext, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception ex) {
            log.warn("Failed to evaluate condition '{}' for assignment {}", expression, assignment.getId(), ex);
            return false;
        }
    }
}
