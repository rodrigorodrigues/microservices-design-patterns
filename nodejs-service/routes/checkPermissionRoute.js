function checkPermissionRoute(router) {
    router.use(function (err, req, res, next) {
        if (err.code === 'permission_denied') {
            res.status(403).send('You do not have permission to access this page');
        }
    });
}

module.exports = checkPermissionRoute;