(function ($) {
  var UILeftNavigation = {
    init: function () {
      var $leftNavi = $('#LeftNavigation');
      this.resize();
      $leftNavi.niceScroll();
    },
    resize: function () {
      var $leftNavi = $('#LeftNavigation');
      var myHeight = $(window).height() - 44;
      $leftNavi.css('height', myHeight + 'px');
      $leftNavi.css('overflow', 'hidden');
    }
  };
  return UILeftNavigation;
})($);