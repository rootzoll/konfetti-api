var gulp = require('gulp'),
    connect = require('gulp-connect');

gulp.task('connect', function() {
    connect.server({
        root: 'src/main/resources/static/konfetti/api/static',
        livereload: true,
        port: 3000
    });
});

gulp.task('html', function () {
    gulp.src('./app/*.html')
        .pipe(connect.reload());
});

gulp.task('watch', function () {
    gulp.watch(['./app/*.html'], ['html']);
    gulp.watch(['./app/*.js'], ['js']);
});

gulp.task('default', ['connect', 'watch']);