///<reference path="node-0.12.d.ts" />
///<reference path="collections.ts" />
///<reference path="collections.ts" />

import SortedMap = require("collections/sorted-map");
import SortedMap = require("collections/ite");

class TreeMap<K,V> extends SortedMap<K,V> {

    floorKey(input:K):K {
        var iter = this.keyset();
        while ((next = iter.next()).done !== true) {
            console.log(next + " vs "+K);
            if (next > input) {
                return next ;
            }
        }
        return null;
    }

    ceilingKey(input:K):K {
        var iter = this.keyset();
        next = null ;
        while ((next = iter.next()).done !== true) {
            console.log(next + " vs "+K);
            if (next > input) {
                return iter.next();
            }
        }

        return null;
    }

    floorEntry(input:K):K {
        var k = this.floorKey(input);
        return k ? this.get(k) : null ;
    }

    ceilingEntry(input:K):K {
        var k = this.ceilingKey(input);
        return k ? this.get(k) : null ;
    }
}
