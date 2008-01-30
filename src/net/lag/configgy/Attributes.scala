package net.lag.configgy

import scala.collection.{mutable, Map}


class AttributesException(reason: String) extends Exception(reason)

protected[configgy] class Cell
protected[configgy] case class StringCell(value: String) extends Cell
protected[configgy] case class AttributesCell(attr: Attributes) extends Cell
protected[configgy] case class StringListCell(array: Array[String]) extends Cell


class Attributes protected[configgy](val root: Config, val name: String) extends AttributeMap {

    private val cells = new mutable.HashMap[String, Cell]
    
    
    def keys: Iterator[String] = cells.keys
    
    override def toString() = {
        val buffer = new StringBuilder("{")
        buffer ++= name
        buffer ++= ": "
        for (val key <- sortedKeys) {
            buffer ++= key
            buffer ++= "="
            buffer ++= (cells(key) match {
                case StringCell(x) => "\"" + StringUtils.quoteC(x) + "\""
                case AttributesCell(x) => x.toString
                case StringListCell(x) => x.mkString("[", ",", "]")
            })
            buffer ++= " "
        }
        buffer ++= "}"
        buffer.toString
    }
    
    override def equals(obj: Any) = {
        if (! obj.isInstanceOf[Attributes]) {
            false
        } else {
            val other = obj.asInstanceOf[Attributes]
            (other.sortedKeys.toList == sortedKeys.toList) && 
                (cells.keys forall (k => { cells(k) == other.cells(k) }))
        }
    }
    
    /**
     * Look up a value cell for a given key. If the key is compound (ie,
     * "abc.xyz"), look up the first segment, and if it refers to an inner
     * Attributes object, recursively look up that cell. If it's not an
     * Attributes or it doesn't exist, return None. For a non-compound key,
     * return the cell if it exists, or None if it doesn't.
     */
    protected[configgy] def lookupCell(key: String): Option[Cell] = {
        val elems = key.split("\\.", 2)
        if (elems.length > 1) {
            cells.get(elems(0)) match {
                // FIXME: i think this cast is exposing a compiler bug?
                case Some(AttributesCell(x)) => x.lookupCell(elems(1)).asInstanceOf[Option[Cell]]
                case _ => None
            }
        } else {
            cells.get(elems(0))
        }
    }
    
    /**
     * Determine if a key is compound (and requires recursion), and if so,
     * return the nested Attributes block and simple key that can be used to
     * make a recursive call. If the key is simple, return None.
     *
     * <p> If the key is compound, but nested Attributes objects don't exist
     * that match the key, an attempt will be made to create the nested
     * Attributes objects. If one of the key segments already refers to an
     * attribute that isn't a nested Attribute object, an AttributesException
     * will be thrown.
     *
     * <p> For example, for the key "a.b.c", the Attributes object for "a.b"
     * and the key "c" will be returned, creating the "a.b" Attributes object
     * if necessary. If "a" or "a.b" exists but isn't a nested Attributes
     * object, then an AttributesException will be thrown.
     */
    @throws(classOf[AttributesException])
    protected[configgy] def recurse(key: String): Option[(Attributes, String)] = {
        val elems = key.split("\\.", 2)
        if (elems.length > 1) {
            val attr = (cells.get(elems(0)) match {
                case Some(AttributesCell(x)) => x
                case Some(_) => throw new AttributesException("Illegal key " + key)
                case None => createNested(elems(0))
            })
            attr.recurse(elems(1)) match {
                case ret @ Some((a, b)) => ret
                case None => Some((attr, elems(1)))
            }
        } else {
            None
        }
    }
    
    private def createNested(key: String): Attributes = {
        val attr = new Attributes(root, if (name.equals("")) key else (name + "." + key))
        cells += key -> new AttributesCell(attr)
        attr
    }
    
    def get(key: String): Option[String] = {
        lookupCell(key) match {
            case Some(StringCell(x)) => Some(x)
            case Some(StringListCell(x)) => Some(x.toList.mkString("[", ",", "]"))
            case _ => None
        }
    }
        
    def getAttributes(key: String): Option[Attributes] = {
        lookupCell(key) match {
            case Some(AttributesCell(x)) => Some(x)
            case _ => None
        }
    }
    
    def getStringList(key: String): Option[Array[String]] = {
        lookupCell(key) match {
            case Some(StringListCell(x)) => Some(x)
            case Some(StringCell(x)) => Some(Array[String](x))
            case _ => None
        }
    }
    
    def set(key: String, value: String): Unit = {
        recurse(key) match {
            case Some((attr, name)) => attr.set(name, value)
            case None => cells.get(key) match {
                case Some(AttributesCell(x)) => throw new AttributesException("Illegal key " + key) 
                case _ => cells += key -> new StringCell(value)
            }
        }
    }
    
    def set(key: String, value: Array[String]): Unit = {
        recurse(key) match {
            case Some((attr, name)) => attr.set(name, value)
            case None => cells.get(key) match {
                case Some(AttributesCell(x)) => throw new AttributesException("Illegal key " + key)
                case _ => cells += key -> new StringListCell(value)
            }
        }
    }
    
    def contains(key: String): Boolean = {
        recurse(key) match {
            case Some((attr, name)) => attr.contains(name)
            case None => cells.contains(key)
        }
    }
    
    def remove(key: String): Boolean = {
        recurse(key) match {
            case Some((attr, name)) => attr.remove(name)
            case None => {
                cells.removeKey(key) match {
                    case Some(_) => true
                    case None => false
                }
            }
        }
    }
    
    def asMap: Map[String, String] = {
        val ret = new mutable.HashMap[String, String]
        for (val (key, value) <- cells) {
            value match {
                case StringCell(x) => ret(key) = x
                case StringListCell(x) => ret(key) = x.mkString("[", ",", "]")
                case AttributesCell(x) => {
                    for (val (k, v) <- x.asMap) {
                        ret(key + "." + k) = v
                    }
                }
            }
        }
        ret
    }
}
