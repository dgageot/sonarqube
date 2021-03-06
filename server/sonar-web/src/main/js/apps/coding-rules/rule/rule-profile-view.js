define([
  'backbone',
  'backbone.marionette',
  './profile-activation-view',
  '../../../components/common/dialogs',
  '../templates'
], function (Backbone, Marionette, ProfileActivationView, confirmDialog) {

  return Marionette.ItemView.extend({
    tagName: 'tr',
    template: Templates['coding-rules-rule-profile'],

    modelEvents: {
      'change': 'render'
    },

    ui: {
      change: '.coding-rules-detail-quality-profile-change',
      revert: '.coding-rules-detail-quality-profile-revert',
      deactivate: '.coding-rules-detail-quality-profile-deactivate'
    },

    events: {
      'click @ui.change': 'change',
      'click @ui.revert': 'revert',
      'click @ui.deactivate': 'deactivate'
    },

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({
        container: 'body'
      });
    },

    change: function () {
      var that = this,
          activationView = new ProfileActivationView({
            model: this.model,
            collection: this.model.collection,
            rule: this.options.rule,
            app: this.options.app
          });
      activationView.on('profileActivated', function () {
        that.options.app.controller.showDetails(that.options.rule);
      });
      activationView.render();
    },

    revert: function () {
      var that = this,
          ruleKey = this.options.rule.get('key');
      confirmDialog({
        title: window.t('coding_rules.revert_to_parent_definition'),
        html: window.tp('coding_rules.revert_to_parent_definition.confirm', this.getParent().name),
        yesHandler: function () {
          return jQuery.ajax({
            type: 'POST',
            url: baseUrl + '/api/qualityprofiles/activate_rule',
            data: {
              profile_key: that.model.get('qProfile'),
              rule_key: ruleKey,
              reset: true
            }
          }).done(function () {
            that.options.app.controller.showDetails(that.options.rule);
          });
        }
      });
    },

    deactivate: function () {
      var that = this,
          ruleKey = this.options.rule.get('key');
      confirmDialog({
        title: window.t('coding_rules.deactivate'),
        html: window.tp('coding_rules.deactivate.confirm'),
        yesHandler: function () {
          return jQuery.ajax({
            type: 'POST',
            url: baseUrl + '/api/qualityprofiles/deactivate_rule',
            data: {
              profile_key: that.model.get('qProfile'),
              rule_key: ruleKey
            }
          }).done(function () {
            that.options.app.controller.showDetails(that.options.rule);
          });
        }
      });
    },

    enableUpdate: function () {
      return this.ui.update.prop('disabled', false);
    },

    getParent: function () {
      if (!(this.model.get('inherit') && this.model.get('inherit') !== 'NONE')) {
        return null;
      }
      var myProfile = _.findWhere(this.options.app.qualityProfiles, {
            key: this.model.get('qProfile')
          }),
          parentKey = myProfile.parentKey,
          parent = _.extend({}, _.findWhere(this.options.app.qualityProfiles, {
            key: parentKey
          })),
          parentActiveInfo = this.model.collection.findWhere({ qProfile: parentKey }) || new Backbone.Model();
      _.extend(parent, parentActiveInfo.toJSON());
      return parent;
    },

    enhanceParameters: function () {
      var parent = this.getParent(),
          params = _.sortBy(this.model.get('params'), 'key');
      if (!parent) {
        return params;
      }
      return params.map(function (p) {
        var parentParam = _.findWhere(parent.params, { key: p.key });
        if (parentParam != null) {
          return _.extend(p, {
            original: _.findWhere(parent.params, { key: p.key }).value
          });
        } else {
          return p;
        }
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite,
        parent: this.getParent(),
        parameters: this.enhanceParameters(),
        templateKey: this.options.rule.get('templateKey'),
        isTemplate: this.options.rule.get('isTemplate')
      });
    }
  });

});
