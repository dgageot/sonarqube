define([
  'components/common/modals',
  'components/common/select-list',
  './templates'
], function (Modal) {

  function getSearchUrl(permission, project) {
    return baseUrl + '/api/permissions/users?ps=100&permission=' + permission + '&projectId=' + project;
  }

  return Modal.extend({
    template: Templates['project-permissions-users'],

    onRender: function () {
      Modal.prototype.onRender.apply(this, arguments);
      new window.SelectList({
        el: this.$('#project-permissions-users'),
        width: '100%',
        readOnly: false,
        focusSearch: false,
        format: function (item) {
          return item.name + '<br><span class="note">' + item.login + '</span>';
        },
        queryParam: 'q',
        searchUrl: getSearchUrl(this.options.permission, this.options.project),
        selectUrl: baseUrl + '/api/permissions/add_user',
        deselectUrl: baseUrl + '/api/permissions/remove_user',
        extra: {
          permission: this.options.permission,
          projectId: this.options.project
        },
        selectParameter: 'login',
        selectParameterValue: 'login',
        parse: function (r) {
          this.more = false;
          return r.users;
        }
      });
    },

    onDestroy: function () {
      if (this.options.refresh) {
        this.options.refresh();
      }
      Modal.prototype.onDestroy.apply(this, arguments);
    },

    serializeData: function () {
      return _.extend(Modal.prototype.serializeData.apply(this, arguments), {
        projectName: this.options.projectName
      });
    }
  });

});
