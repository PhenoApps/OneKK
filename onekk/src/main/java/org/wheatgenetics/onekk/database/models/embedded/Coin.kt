package org.wheatgenetics.onekk.database.models.embedded

/** Model of circular reference objects used for measuring, named Coin. **/

data class Coin(
    var diameter: String? = null,
    var name: String? = null
) {

    override fun toString(): String {

        return "$diameter,$name"

    }
}