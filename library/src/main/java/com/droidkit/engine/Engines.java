package com.droidkit.engine;

import com.droidkit.actors.ActorSystem;
import com.droidkit.actors.android.UiActorDispatcher;
import com.droidkit.actors.mailbox.ActorDispatcher;

/**
 * Created by ex3ndr on 28.08.14.
 */
public class Engines {
    public static void init() {
        ActorSystem.system().addDispatcher("db", new ActorDispatcher(ActorSystem.system(), 1, Thread.MIN_PRIORITY));
        ActorSystem.system().addDispatcher("ui", new UiActorDispatcher(ActorSystem.system()));
    }
}
