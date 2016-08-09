import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Calendar;
import java.util.List;

import static java.lang.String.format;

public class App {

    public static void main(String... args) throws InterruptedException {

        String url = "https://api.github.com";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        final GitHubService service = retrofit.create(GitHubService.class);

        go(service);
    }

    private static void go(GitHubService service) throws InterruptedException {
        long start = Calendar.getInstance().getTimeInMillis();
        Observable.just("")
                .flatMap(o -> {
                    System.out.println("## getUsers: " + Thread.currentThread().getName());
                    return service.getUsers();
                })
                .flatMap((iterable) -> {
                    System.out.println("## flatMap to users: " + Thread.currentThread().getName());
                    return Observable.from(iterable);
                })
                .take(20)
                .map((gitHub1) -> {
                    System.out.println("## get user login: " + Thread.currentThread().getName());
                    return gitHub1.getLogin();
                })
                .flatMap((user) -> {
                    System.out.println("## flatMap to findUser: " + Thread.currentThread().getName());
                    return service.findUser(user);
                })
                .doOnCompleted(() -> {
                    System.out.println("## onComplete: " + Thread.currentThread().getName());
                    System.out.println(format("## Completed in %d ms", Calendar.getInstance().getTimeInMillis() - start));
                })
                .doOnError(Throwable::printStackTrace)
                .doOnNext(gitHub -> {
                    System.out.println("## onNext: " + Thread.currentThread().getName());
                    System.out.println(gitHub.getName());
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe();
        System.out.println("## Waiting for a response...");
        Thread.sleep(20000);
        System.out.println("## Done\n");
    }

    private interface GitHubService {

        @Headers(value = {
                "Authorization: Basic Z2Vla2FyaXN0OjE2MjYwZWZlMzY2MzQzY2VlMTRjYTU1YWY1ODMwNDdjMjg1ZDBiZDU=",
                "Cache-Control: no-cache, must-revalidate",
                "Pragma: no-cache",
                "Expires: Sat, 26 Jul 1997 05:00:00 GMT"
        })
        @GET(value = "/users")
        Observable<List<GitHub>> getUsers();

        @Headers(value = {
                "Authorization: Basic Z2Vla2FyaXN0OjE2MjYwZWZlMzY2MzQzY2VlMTRjYTU1YWY1ODMwNDdjMjg1ZDBiZDU=",
                "Cache-Control: no-cache, must-revalidate",
                "Pragma: no-cache",
                "Expires: Sat, 26 Jul 1997 05:00:00 GMT"
        })
        @GET(value = "/users/{login}")
        Observable<GitHub> findUser(@Path("login") String user);
    }

    private static class GitHub {

        private String name;
        private String login;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }
    }
}
