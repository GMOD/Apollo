///<reference path="node-0.12.d.ts" />
///<reference path="collections.ts" />

import SortedMap = require("collections/sorted-map");

class TreeMap<K,V> extends SortedMap<K,V>{

   floorKey(input:K):K {
      return null ;
   }
   ceilingKey(input:K):K {
      return null ;
   }

   floorEntry(input:K):K {
      return null ;
   }
   ceilingEntry(input:K):K {
      return null ;
   }
}
