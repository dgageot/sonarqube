define([
  './custom-values-facet'
], function (CustomValuesFacet) {

  return CustomValuesFacet.extend({
    getUrl: function () {
      return window.baseUrl + '/api/issues/authors';
    },

    prepareSearch: function () {
      return this.$('.js-custom-value').select2({
        placeholder: 'Search...',
        minimumInputLength: 2,
        allowClear: false,
        formatNoMatches: function () {
          return window.t('select2.noMatches');
        },
        formatSearching: function () {
          return window.t('select2.searching');
        },
        formatInputTooShort: function () {
          return window.tp('select2.tooShort', 2);
        },
        width: '100%',
        ajax: {
          quietMillis: 300,
          url: this.getUrl(),
          data: function (term) {
            return { q: term, ps: 25 };
          },
          results: function (data) {
            return {
              more: false,
              results: data.authors.map(function (author) {
                return { id: author, text: author };
              })
            };
          }
        }
      });
    }
  });

});
