package com.marketinghub.mds.search;

import com.marketinghub.mds.dto.BackendMdsRequestDto;
import org.springframework.stereotype.Component;

@Component
public class MechanismQuestionBuilder {
    public MechanismQuestion build(BackendMdsRequestDto request) {
        String text = "Quais mecanismos explicam como " + request.problem()
                + " no mercado " + request.market()
                + " impacta o desfecho desejado \"" + request.desiredOutcome() + "\"?";
        return new MechanismQuestion(text);
    }
}
