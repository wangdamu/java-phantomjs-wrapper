package com.moodysalem.java;

import com.moodysalem.phantomjs.wrapper.PhantomJS;
import com.moodysalem.phantomjs.wrapper.RenderException;
import com.moodysalem.phantomjs.wrapper.beans.BannerInfo;
import com.moodysalem.phantomjs.wrapper.beans.PhantomJSExecutionResponse;
import com.moodysalem.phantomjs.wrapper.beans.PhantomJSOptions;
import com.moodysalem.phantomjs.wrapper.beans.RenderOptions;
import com.moodysalem.phantomjs.wrapper.enums.SizeUnit;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.AssertJUnit.assertTrue;

public class TestPhantomJS {
    private static final Logger LOG = Logger.getAnonymousLogger();

    @Test
    public void testRunScript() throws IOException, InterruptedException {
        final PhantomJSExecutionResponse phantomJSExecutionResponse =
            PhantomJS.exec(TestPhantomJS.class.getResourceAsStream("test-script.js"));

        assertTrue(phantomJSExecutionResponse.getExitCode() == 3);
        assertTrue(phantomJSExecutionResponse.getStdErr().isEmpty());
        assertTrue(!phantomJSExecutionResponse.getStdOut().isEmpty());
    }

    @Test
    public void testOptions() throws IOException {
        assertTrue(PhantomJS.exec(TestPhantomJS.class.getResourceAsStream("test-script.js"), PhantomJSOptions.DEFAULT.withHelp(true)).getExitCode() == 0);
        assertTrue(PhantomJS.exec(TestPhantomJS.class.getResourceAsStream("test-script.js"), PhantomJSOptions.DEFAULT.withVersion(true)).getExitCode() == 0);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testNullPointerExWithEmpty() throws IOException, RenderException {
        PhantomJS.render(TestPhantomJS.class.getResourceAsStream("test-js-waiting.html"), RenderOptions.EMPTY);
    }

    @Test
    public void testRender() throws IOException, RenderException {
        try (InputStream is = PhantomJS.render(TestPhantomJS.class.getResourceAsStream("test-js-waiting.html"), RenderOptions.DEFAULT)) {
            PDDocument doc = PDDocument.load(is);
            doc.close();
            assertTrue(doc.getNumberOfPages() == 1);
        }
    }

    @Test
    public void testWithExternalCss() throws IOException, RenderException {
        try (InputStream is = PhantomJS.render(TestPhantomJS.class.getResourceAsStream("test-with-resources.html"), RenderOptions.DEFAULT)) {
            PDDocument doc = PDDocument.load(is);
            doc.close();
            assertTrue(doc.getNumberOfPages() == 1);
        }
    }

    @Test
    public void testRenderJsWait() throws IOException, RenderException {
        try (InputStream is = PhantomJS.render(TestPhantomJS.class.getResourceAsStream("test-js-waiting.html"), RenderOptions.DEFAULT)) {
            PDDocument doc = PDDocument.load(is);
            doc.close();
            assertTrue(doc.getNumberOfPages() == 1);
        }
    }

    @Test(expectedExceptions = RenderException.class)
    public void testRenderJsWaitTimeout() throws IOException, RenderException {
        try (InputStream is = PhantomJS.render(TestPhantomJS.class.getResourceAsStream("test-js-waiting.html"), RenderOptions.DEFAULT.withJavaScriptExecutionDetails(100L, 50L))) {
            PDDocument doc = PDDocument.load(is);
            doc.close();
            assertTrue(doc.getNumberOfPages() == 1);
        }
    }


    @Test
    public void testFooterAndHeader() throws IOException, RenderException {
        BannerInfo header = new BannerInfo(1, SizeUnit.in, "function (pageNum, numPages) { return \"<h5>Header: <span style='float:right'>\" + ['zero','one','two'][pageNum] + \" / \" + ['zero','one','two'][numPages] + \"</span></h5>\"; }");
        BannerInfo footer = new BannerInfo(1, SizeUnit.in, "function (pageNum, numPages) { return \"<h5>Footer: <span style='float:right'>\" + pageNum + \" / \" + numPages + \"</span></h5>\"; }");

        try (InputStream is = PhantomJS.render(TestPhantomJS.class.getResourceAsStream("test.html"),
            RenderOptions.DEFAULT.withHeaderInfo(header).withFooterInfo(footer))) {
            PDDocument doc = PDDocument.load(is);
            doc.close();
            assertTrue(doc.getNumberOfPages() == 2);
        }
    }

    @Test
    public void concurrencyTest() throws InterruptedException {
        final List<Thread> threads = new LinkedList<>();

        for (int i = 0; i < 10; i++) {
            threads.add(newRenderThread());
        }

        threads.forEach(Thread::start);

        // wait 10 seconds and none of the threads should be interrupted
        Thread.sleep(10000);

        if (threads.stream().anyMatch(Thread::isInterrupted)) {
            throw new InterruptedException("Load test has encountered a thread InterruptedException. Please refer to log file for details.");
        }
    }

    /**
     * Returns a thread that renders test.html
     *
     * @return the thread
     */
    private Thread newRenderThread() {
        return new Thread() {
            public void run() {
                try (
                    final InputStream pdf = PhantomJS.render(
                        TestPhantomJS.class.getResourceAsStream("test.html"),
                        RenderOptions.DEFAULT
                    )
                ) {
                    assert IOUtils.toByteArray(pdf).length > 0;
                    LOG.log(Level.INFO, "Thread finished rendering: " + getName());
                } catch (Exception e) {
                    this.interrupt();
                    e.printStackTrace();
                }
            }
        };
    }
}
