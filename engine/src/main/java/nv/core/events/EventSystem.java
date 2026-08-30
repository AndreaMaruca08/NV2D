package nv.core.events;

import nv.core.annotations.EngineCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <h2>NV2D Event System</h2>
 * <p>Handles events between different components for better communication</p>
 * @since 1.7.0
 */
@EngineCore
@SuppressWarnings("unused")
public final class EventSystem {

    private final Map<Class<?>, ListenerList<?>> listeners = new HashMap<>();

    public <T extends NvEvent> void on(
            Class<T> type,
            Consumer<T> listener
    ) {
        getOrCreate(type).add(listener);
    }

    public <T extends NvEvent> void emit(
            Class<T> type,
            T event
    ) {
        get(type).emit(event);
    }

    private <T extends NvEvent> ListenerList<T> getOrCreate(
            Class<T> type
    ) {
        return getTyped(listeners.computeIfAbsent(type, ignored -> new ListenerList<>()));
    }

    private <T extends NvEvent> ListenerList<T> get(
            Class<T> type
    ) {
        ListenerList<?> list = listeners.get(type);

        if (list == null) {
            return new ListenerList<>();
        }

        return getTyped(list);
    }

    @SuppressWarnings("unchecked")
    private static <T extends NvEvent> ListenerList<T> getTyped(
            ListenerList<?> list
    ) {
        return (ListenerList<T>) list;
    }

    private static final class ListenerList<T extends NvEvent> {

        private final List<Consumer<T>> listeners = new ArrayList<>();

        void add(Consumer<T> listener) {
            listeners.add(listener);
        }

        void emit(T event) {
            List<Consumer<T>> snapshot = List.copyOf(listeners);

            snapshot.forEach(listener -> listener.accept(event));
        }
    }
}