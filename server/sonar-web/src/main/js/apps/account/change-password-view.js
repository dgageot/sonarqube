define([
  'components/common/modal-form',
  './templates'
], function (ModalForm) {

  var $ = jQuery;

  return ModalForm.extend({
    template: Templates['account-change-password'],

    onFormSubmit: function () {
      ModalForm.prototype.onFormSubmit.apply(this, arguments);
      if (this.checkPasswords()) {
        this.sendRequest();
      } else {
        this.showErrors([{ msg: window.t('user.password_doesnt_match_confirmation') }]);
      }
    },

    checkPasswords: function () {
      var p1 = this.$('#password').val(),
          p2 = this.$('#password_confirmation').val();
      return p1 === p2;
    },

    sendRequest: function () {
      var that = this;
      var data = {
        login: window.SS.user,
        password: this.$('#password').val(),
        previousPassword: this.$('#old_password').val()
      };
      var opts = {
        type: 'POST',
        url: window.baseUrl + '/api/users/change_password',
        data: data,
        statusCode: {
          // do not show global error
          400: null
        }
      };
      this.disableForm();
      $.ajax(opts).done(function () {
        that.destroy();
      }).fail(function (jqXHR) {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    }
  });

});
