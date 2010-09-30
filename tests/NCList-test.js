describe("NCList",
function () {
    var testFeats = [[2, 5, "b"], [20, 30, "d"], [5, 15, "c"], [1, 10, "a"]];

    var ncl;

    beforeEach(function() {
                   ncl = new NCList();
                   ncl.setSublistIndex(3);
               });

    it("should allow feature addition", function() {
           ncl.add(testFeats[0], "tf-zero");
           var feats = [];
           ncl.iterate(-Infinity, Infinity, function(f) { feats.push(f); });
           expect(feats.length).toEqual(1);
           expect(feats[0]).toBe(testFeats[0]);
       });

    it("should allow feature deletion", function() {
           ncl.add(testFeats[0], "tf-zero");
           ncl.delete("tf-zero");
           var feats = [];
           ncl.iterate(-Infinity, Infinity, function(f) { feats.push(f); });
           expect(feats.length).toEqual(0);
       });

    it("should iterate in the right order after additions", function() {
           for (var i = 0; i < testFeats.length; i++)
               ncl.add(testFeats[i], "tf-" + i);
           var feats = [];
           ncl.iterate(-Infinity, Infinity, function(f) { feats.push(f); });
           expect(feats[0]).toBe(testFeats[3]);
           expect(feats[1]).toBe(testFeats[0]);
           expect(feats[2]).toBe(testFeats[2]);
           expect(feats[3]).toBe(testFeats[1]);
       });

    it("should allow deletion of a container feature while keeping containees",
       function() {
           for (var i = 0; i < testFeats.length; i++)
               ncl.add(testFeats[i], "tf-" + i);
           // tf-3 will contain tf-0; after we delete tf-3, we should still
           // have all the rest of the features
           ncl.delete("tf-3");
           var feats = [];
           ncl.iterate(-Infinity, Infinity, function(f) { feats.push(f); });
           expect(feats[0]).toBe(testFeats[0]);
           expect(feats[1]).toBe(testFeats[2]);
           expect(feats[2]).toBe(testFeats[1]);
       });
});