define([
        'dojo/_base/declare',
        'dojo/_base/lang',
        'dojo/dom-construct',
        'dojo/promise/all',
        'JBrowse/Util',
        'JBrowse/View/Track/_VariantDetailMixin'
    ],
    function(
        declare,
        lang,
        domConstruct,
        all,
        Util,
        VariantDetailMixin
    ) {

return declare( VariantDetailMixin, {

    _renderGenotypes: function( parentElement, track, f, featDiv  ) {
        var thisB = this;
        var genotypes;
        var hasGenotypes = f.get('genotypes');
        var isRestStore = track.config.storeClass === "WebApollo/Store/SeqFeature/VCFTabixREST";

        if (hasGenotypes) {
            if (isRestStore) {
                this.store.requestGenotypes(f).then(function(data) {
                    genotypes = data;
                    if (!genotypes) return;
                    thisB._renderVariantGenotypes(parentElement, track, f, genotypes, featDiv);
                });
            }
            else {
                thisB._renderVariantGenotypes(parentElement, track, f, f.get('genotypes'), featDiv);
            }
        }
    },

    _renderVariantGenotypes: function(parentElement, track, f, genotypes, featDiv) {
        var thisB = this;
        var keys = Util.dojof.keys(genotypes).sort();
        var gCount = keys.length;
        if (!gCount)
            return;

        // get variants and coerce to an array
        var alt = f.get('alternative_alleles');
        if (alt && typeof alt == 'object' && 'values' in alt)
            alt = alt.values;
        if (alt && !lang.isArray(alt))
            alt = [alt];

        var gContainer = domConstruct.create(
            'div',
            {
                className: 'genotypes',
                innerHTML: '<h2 class="sectiontitle">Genotypes ('
                + gCount + ')</h2>'
            },
            parentElement);


        function render(underlyingRefSeq) {
            var summaryElement = thisB._renderGenotypeSummary(gContainer, genotypes, alt, underlyingRefSeq);

            var valueContainer = domConstruct.create(
                'div',
                {
                    className: 'value_container genotypes'
                }, gContainer);

            thisB.renderDetailValueGrid(
                valueContainer,
                'Genotypes',
                f,
                // iterator
                function () {
                    if (!keys.length)
                        return null;
                    var k = keys.shift();
                    var value = genotypes[k];
                    var item = {id: k};
                    for (var field in value) {
                        item[field] = thisB._mungeGenotypeVal(value[field], field, alt, underlyingRefSeq);
                    }
                    return item;
                },
                {
                    descriptions: (function () {
                        if (!keys.length)
                            return {};

                        var subValue = genotypes[keys[0]];
                        var descriptions = {};
                        for (var k in subValue) {
                            descriptions[k] = subValue[k].meta && subValue[k].meta.description || null;
                        }
                        return descriptions;
                    })()
                }
            );
        };

        track.browser.getStore('refseqs', function (refSeqStore) {
            if (refSeqStore) {
                refSeqStore.getReferenceSequence(
                    {
                        ref: track.refSeq.name,
                        start: f.get('start'),
                        end: f.get('end')
                    },
                    render,
                    function () {
                        render();
                    }
                );
            }
            else {
                render();
            }
        });
    }
});
});