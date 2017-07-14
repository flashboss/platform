(function ($, base) {
  var UILeftNavigation = {
    init: function () {
      var $leftNavi = $('#LeftNavigation');
      this.resize();
      if ($(window).width()  > 1024) {
        $leftNavi.perfectScrollbar();
      }
      base.Browser.addOnResizeCallback('UILeftNavigation', function() {
        var $leftNavi = $('#LeftNavigation');
        if ($(window).width()  > 1024) {
          alert('update');
          $leftNavi.css('position', 'fixed').perfectScrollbar();
        } else {
          alert('destroy');
          $leftNavi.css('position', 'static').perfectScrollbar('destroy');
        }
      });
    },
    resize: function () {
      var $leftNavi = $('#LeftNavigation');
      var myHeight = $(window).height() - 44;
      $leftNavi.css('height', myHeight + 'px');
    }
  };
  return UILeftNavigation;
})($);