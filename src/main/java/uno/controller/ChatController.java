package uno.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import uno.dto.MessageChat;
import java.time.LocalDateTime;

@Controller
public class ChatController {

    @MessageMapping("/chat.envoyer/{codeSalon}")
    @SendTo("/topic/chat/{codeSalon}")
    public MessageChat envoyerMessage(@DestinationVariable String codeSalon, MessageChat message) {
        if (message.getDateEnvoi() == null) {
            message.setDateEnvoi(LocalDateTime.now());
        }
        return message;
    }
}