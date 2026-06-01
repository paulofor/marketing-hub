package com.marketinghub.whatsapp.web;

import com.marketinghub.whatsapp.WhatsAppMessage;
import com.marketinghub.whatsapp.WhatsAppMessageDirection;
import com.marketinghub.whatsapp.WhatsAppMessageType;
import com.marketinghub.whatsapp.dto.WhatsAppMessageDto;
import com.marketinghub.whatsapp.dto.WhatsAppSendMessageRequest;
import com.marketinghub.whatsapp.mapper.WhatsAppMessageMapper;
import com.marketinghub.repository.jpa.whatsapp.WhatsAppMessageRepository;
import com.marketinghub.whatsapp.service.WhatsAppMessagingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller exposing WhatsApp message logs and manual sending.
 */
@RestController
@RequestMapping("/api/whatsapp/messages")
public class WhatsAppMessageController {
    private final WhatsAppMessageRepository messageRepository;
    private final WhatsAppMessageMapper messageMapper;
    private final WhatsAppMessagingService messagingService;

    public WhatsAppMessageController(WhatsAppMessageRepository messageRepository,
                                     WhatsAppMessageMapper messageMapper,
                                     WhatsAppMessagingService messagingService) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.messagingService = messagingService;
    }

    @GetMapping
    public Page<WhatsAppMessageDto> listMessages(@RequestParam(name = "page", defaultValue = "0") int page,
                                                 @RequestParam(name = "size", defaultValue = "25") int size,
                                                 @RequestParam(name = "direction", required = false) WhatsAppMessageDirection direction) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<WhatsAppMessage> result = direction == null
                ? messageRepository.findAllByOrderByCreatedAtDesc(pageable)
                : messageRepository.findByDirectionOrderByCreatedAtDesc(direction, pageable);
        return result.map(messageMapper::toDto);
    }

    @PostMapping("/send")
    public WhatsAppMessageDto sendMessage(@RequestBody WhatsAppSendMessageRequest request) {
        if (!StringUtils.hasText(request.getTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination phone is required");
        }
        WhatsAppMessageType type = request.getType() != null ? request.getType() : WhatsAppMessageType.TEXT;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "console");
        try {
            WhatsAppMessage message;
            switch (type) {
                case IMAGE -> {
                    if (!StringUtils.hasText(request.getImageUrl())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL is required for image messages");
                    }
                    message = messagingService.sendImageMessage(request.getTo(), request.getImageUrl(), request.getCaption(), metadata);
                }
                case TEXT -> {
                    if (!StringUtils.hasText(request.getTextBody())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required for text messages");
                    }
                    message = messagingService.sendTextMessage(request.getTo(), request.getTextBody(), metadata);
                }
                case TEMPLATE -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template messages are not supported via console");
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported message type: " + type);
            }
            return messageMapper.toDto(message);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

}
