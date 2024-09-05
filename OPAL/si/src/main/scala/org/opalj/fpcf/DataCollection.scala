package org.opalj.fpcf

import scala.collection.mutable

object DataCollection {
    private val dataCollection: mutable.Map[String, Any] = mutable.Map.empty

    def add(key: String, value: Any): Unit = {
        dataCollection += (key -> value)
        //print("ADDED")
    }

    def get(key: String): Option[Any] = {
        dataCollection.get(key)
    }

    def remove(key: String): Unit = {
        dataCollection -= key
    }

    def clear(): Unit = {
        dataCollection.clear()
    }

    def getAll: Map[String, Any] = {
        dataCollection.toMap
    }

}
