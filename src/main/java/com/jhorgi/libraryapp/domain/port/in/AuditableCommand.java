package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.Actor;

/**
 * A command that names the caller it was issued by.
 *
 * <p>Exists so {@code AuditAspect} can read the actor off a command object
 * without reflecting over accessor names. Reflection would work right up until
 * someone renamed {@code requester()}, at which point audit rows would quietly
 * start coming out anonymous instead of failing to compile.
 */
public interface AuditableCommand {

    Actor requester();
}
