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
        ListenerList<T> list = get(type);
        if (list != null) {
            list.emit(event);
        }
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
        return list != null ? getTyped(list) : null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends NvEvent> ListenerList<T> getTyped(
            ListenerList<?> list
    ) {
        return (ListenerList<T>) list;
    }

    private static final class ListenerList<T extends NvEvent> {

        private final List<Consumer<T>> listeners = new ArrayList<>();
        @SuppressWarnings("unchecked")
        private volatile Consumer<T>[] snapshot = (Consumer<T>[]) new Consumer[0];

        @SuppressWarnings("unchecked")
        synchronized void add(Consumer<T> listener) {
            listeners.add(listener);
            snapshot = listeners.toArray((Consumer<T>[]) new Consumer[listeners.size()]);
        }

        void emit(T event) {
            Consumer<T>[] localSnapshot = this.snapshot;
            for (int i = 0; i < localSnapshot.length; i++) {
                localSnapshot[i].accept(event);
            }
        }
    }
}