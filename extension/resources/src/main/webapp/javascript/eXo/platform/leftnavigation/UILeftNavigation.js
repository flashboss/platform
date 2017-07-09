(function ($) {
  var $leftNavi = $('#LeftNavigation');

  var UILeftNavigation = {
    init: function () {
      this.resize();
      $leftNavi.niceScroll();
    },
    resize: function () {
      var myHeight = $(window).height() - 44;
      $leftNavi.css('height', myHeight + 'px');
      $leftNavi.css('overflow', 'hidden');
    }
  };
  return UILeftNavigation;
})($);