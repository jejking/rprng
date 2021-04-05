package com.jejking.rprng.rng

import scala.annotation.tailrec

/**
 * Enhances a traversable with method to map it to a set of a target size..
 */
class WithToSizedSet[T](iterable: Iterable[T]) {

  /**
   * Creates a set of the target size from the traversable. If a set of the target size can
   * be created from the elements of the traversable and there are more available, these are ignored.
   * If the traversable has fewer elements constituting a set than the target size, then the largest
   * possible set below the target size will be returned.
   *
   * @param targetSize zero or greater. It generally only makes sense to use an integer > 1.
   * @return a set which may be empty.
   */
  def toSizedSet(targetSize: Int): Set[T] = {
    require(targetSize >= 0)

    @tailrec def addToSet(accum: Set[T], iterating: Iterable[T]): Set[T] = {
      if (accum.size == targetSize) {
        accum
      } else if (iterating.headOption.isEmpty) {
        accum
      } else {
        addToSet(accum + iterating.head, iterating.tail)
      }

    }

    addToSet(Set.empty, iterable)
  }

}
