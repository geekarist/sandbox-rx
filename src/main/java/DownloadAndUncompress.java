import okhttp3.ResponseBody;
import org.tukaani.xz.XZInputStream;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Streaming;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

public class DownloadAndUncompress {

    private interface Api {
        @GET("/pub/linux/kernel/v4.x/{path}")
        @Streaming
        Observable<ResponseBody> get(@Path("path") String path);
    }

    public static void main(String... args) throws InterruptedException {

        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl("http://linux-kernel.uio.no/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        Api api = restAdapter.create(Api.class);

        System.out.println("Starting: " + new Date());

        Observable.just("linux-4.0.1.tar", "linux-4.0.2.tar", "linux-4.0.99.tar")
                .flatMap(name -> downloadObservable(api, name + ".xz", name))
                .doOnCompleted(() -> System.exit(0))
                .subscribe();

        Thread.sleep(600000);
    }

    private static Observable<byte[]> downloadObservable(Api api,
                                                         String compressedName,
                                                         String uncompressedName) {
        return api.get(compressedName)
                .subscribeOn(Schedulers.io())
                .map(responseBody -> responseToBytes(responseBody))
                .observeOn(Schedulers.computation())
                .map(inputBytes -> uncompress(inputBytes))
                .observeOn(Schedulers.io())
                .doOnNext(bytes -> writeToDisk(bytes, uncompressedName))
                .doOnError(error -> log(String.valueOf(error)))
                .onErrorReturn((error) -> new byte[0]);
    }

    static byte[] uncompress(byte[] inputBytes) {
        log("uncompress");
        try (XZInputStream xzInputStream = new XZInputStream(new BufferedInputStream(new ByteArrayInputStream(inputBytes)));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while (-1 != (n = xzInputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeToDisk(byte[] bytes, String name) {
        log("writeToDisk");
        try {
            Files.write(Paths.get(name), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] responseToBytes(ResponseBody responseBody) {
        log("responseToBytes");
        try {
            return responseBody.bytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void log(String fun) {
        System.out.println(fun + ": " + new Date() + ": " + Thread.currentThread().getName());
    }
}
