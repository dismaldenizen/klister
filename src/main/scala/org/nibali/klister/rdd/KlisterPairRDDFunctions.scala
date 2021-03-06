package org.nibali.klister.rdd

import org.nibali.klister.Klister._
import scala.reflect.ClassTag
import org.apache.spark.rdd.RDD
import org.apache.spark.Logging
import org.apache.spark.SparkContext._
import org.nibali.klister.regionmaps._

/**
* Extra functions on pair RDDs provided by Klister through an implicit
* conversion. Import `org.nibali.klister.Klister._` at the top of your program
* to use these functions.
*/
class KlisterPairRDDFunctions[K, V](self: RDD[(K, V)])
    (implicit kt: ClassTag[K], vt: ClassTag[V], ord: Ordering[K] = null)
  extends Logging
  with Serializable
{
  def thetaJoin[W](other: RDD[(K, W)], joinCond:((K, K) => Boolean),
      nReducers:Int = 1):RDD[((K, V),(K, W))] = {
    val srm = SimpleRegionMapper(self, other, nReducers)
    return _thetaJoin(other, joinCond, srm)
  }

  private[klister] def _thetaJoin[W](other: RDD[(K, W)], joinCond:((K, K) => Boolean),
      rm:RegionMapper[(K,V),(K,W)]):RDD[((K, V),(K, W))] = {
    return self._kartesian(other, rm).filter(x => joinCond.apply(x._1._1, x._2._1))
  }

  def inequalityJoin[W](other: RDD[(K, W)], op: Comparison.Comparison,
      nReducers:Int = 1):RDD[((K, V),(K, W))] = {
    if(ord == null) {
      throw new RuntimeException("Key type does not have an Ordering")
    }
    val rm = ComparatorRegionMapper(self, other, op, nReducers)
    return _thetaJoin(other, (a, b) => op.get.contains(ord.compare(a, b)), rm)
  }

  def equijoin[W](other: RDD[(K, W)], nReducers:Int = 1):RDD[(K,(V, W))] = {
    return inequalityJoin(other, Comparison.Eq, nReducers).
      map(x => (x._1._1, (x._1._2, x._2._2)))
  }
}
