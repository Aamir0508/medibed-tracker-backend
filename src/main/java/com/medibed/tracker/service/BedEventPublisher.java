package com.medibed.tracker.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.medibed.tracker.dto.Dtos.HospitalResponse;

/** Server-Sent Events: pushes every bed-count change to all connected browsers (the "Live Bed Feed"). */
@Component
public class BedEventPublisher {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(t -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void publish(HospitalResponse hospital) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("bed-update").data(hospital, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
