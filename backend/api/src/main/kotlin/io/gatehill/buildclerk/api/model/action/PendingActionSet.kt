package io.gatehill.buildclerk.api.model.action

import java.util.UUID

/**
 * Identifies a set of pending actions.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class PendingActionSet {
    val id: String = UUID.randomUUID().toString()
    val actions = mutableListOf<PendingAction>()
}
