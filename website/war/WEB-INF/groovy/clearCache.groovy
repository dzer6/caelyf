html.ul {
    [
            "/",
            "/tutorial",
            "/tutorial/setup",
            "/tutorial/views-and-controllers",
            "/tutorial/url-routing",
            "/tutorial/plugins",
            "/tutorial/run-deploy",
            "/download",
            "/community"
    ].each {
        redis.del it
        li "Cleared cache for URI: $it"
    }
}
