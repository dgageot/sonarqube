define([
  'backbone.marionette',
  './profile-view',
  './profiles-empty-view',
  './templates'
], function (Marionette, ProfileView, ProfilesEmptyView) {

  return Marionette.CompositeView.extend({
    className: 'list-group',
    template: Templates['quality-profiles-profiles'],
    languageTemplate: Templates['quality-profiles-profiles-language'],
    childView: ProfileView,
    childViewContainer: '.js-list',
    emptyView: ProfilesEmptyView,

    collectionEvents: {
      'filter': 'filterByLanguage'
    },

    childViewOptions: function (model) {
      return {
        collectionView: this,
        highlighted: model.get('key') === this.highlighted
      };
    },

    highlight: function (key) {
      this.highlighted = key;
      this.render();
    },

    attachHtml: function (compositeView, childView, index) {
      var $container = this.getChildViewContainer(compositeView),
          model = this.collection.at(index);
      if (model != null) {
        var prev = this.collection.at(index - 1),
            putLanguage = prev == null;
        if (prev != null) {
          var lang = model.get('language'),
              prevLang = prev.get('language');
          if (lang !== prevLang) {
            putLanguage = true;
          }
        }
        if (putLanguage) {
          $container.append(this.languageTemplate(model.toJSON()));
        }
      }
      compositeView._insertAfter(childView);
    },

    closeChildren: function () {
      Marionette.CompositeView.prototype.closeChildren.apply(this, arguments);
      this.$('.js-list-language').remove();
    },

    filterByLanguage: function (language) {
      if (language) {
        this.$('[data-language]').addClass('hidden');
        this.$('[data-language="' + language + '"]').removeClass('hidden');
      } else {
        this.$('[data-language]').removeClass('hidden');
      }
    }
  });

});
