package com.example.parking.global.sse

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class SseEmitterManager {

    // parkingLotId → 연결된 Emitter 목록
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    fun subscribe(parkingLotId: Long): SseEmitter {
        val emitter = SseEmitter(60 * 60 * 1000L) // 1시간 타임아웃

        emitters.compute(parkingLotId) { _, list ->
            (list ?: CopyOnWriteArrayList()).also { it.add(emitter) }
        }

        // 연결 종료 시 제거
        emitter.onCompletion { remove(parkingLotId, emitter) }
        emitter.onTimeout   { remove(parkingLotId, emitter) }
        emitter.onError     { remove(parkingLotId, emitter) }

        // 최초 연결 시 더미 이벤트 (503 방지)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"))
        } catch (e: Exception) {
            remove(parkingLotId, emitter)
        }

        return emitter
    }

    fun notify(parkingLotId: Long, data: Any) {
        val list = emitters[parkingLotId] ?: return
        val dead = mutableListOf<SseEmitter>()

        list.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().data(data))
            } catch (e: Exception) {
                dead.add(emitter)
                emitter.completeWithError(e)
            }
        }
        dead.forEach { remove(parkingLotId, it) }
    }

    private fun remove(parkingLotId: Long, emitter: SseEmitter) {
        emitters.computeIfPresent(parkingLotId) { _, list ->
            list.remove(emitter)
            if (list.isEmpty()) null else list  // null 반환 시 map에서 키 자체가 삭제됨
        }
    }
}