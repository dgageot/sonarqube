define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.ItemView.extend({
    template: Templates['computation-search'],

    collectionEvents: {
      'all': 'render'
    },

    events: {
      'click .js-queue': 'queue',
      'click .js-history': 'history'
    },

    queue: function (e) {
      e.preventDefault();
      this.options.router.navigate('current', { trigger: true });
    },

    history: function (e) {
      e.preventDefault();
      this.options.router.navigate('past', { trigger: true });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        tab: this.collection.q
      });
    }
  });

});
