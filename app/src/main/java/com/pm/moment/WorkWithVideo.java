package com.pm.moment;

import android.os.Environment;
import android.util.Log;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceViaHeapImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import org.jcodec.api.JCodecException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class WorkWithVideo {

    private static final String videoFolderPath = Environment.getExternalStorageDirectory() + "/Collapse/";

    public static File init(List<String> videoUris) throws IOException, JCodecException {
        WorkWithFiles.checkDefaultDir(videoFolderPath);
        List<Movie> movies = initVideos(videoUris);
        if (movies != ResultsCodes.INIT_VIDEO_NOT_EXIST) {
            List<Track> audioTracks = getAudioTracks(movies);
            List<String> newVideosPaths = getGetNewVideosPaths(audioTracks, videoUris);
            movies = initVideos(newVideosPaths);
            audioTracks = getAudioTracks(movies);
            List<Track> videoTracks = getVideoTracks(movies);
            return concat(audioTracks, videoTracks, newVideosPaths);
        }
        return ResultsCodes.ERROR_CREATE_VIDEO;
    }

    private static List<Movie> initVideos(List<String> videoUris) throws IOException, JCodecException {
        List<Movie> inMovies = new ArrayList<Movie>();
        for (String videoUri : videoUris) {
            File video = new File(videoUri);
            if (!video.exists()) {
                return ResultsCodes.INIT_VIDEO_NOT_EXIST;
            }
            inMovies.add(MovieCreator.build(videoUri));
        }
        return inMovies;
    }

    private static List<Track> getAudioTracks(List<Movie> inMovies) throws IOException {
        List<Track> audioTracks = new LinkedList<Track>();
        for (Movie movie : inMovies) {
            for (Track track : movie.getTracks()) {
                if (track.getHandler().equals("soun")) {
                    audioTracks.add(track);
                }
            }
        }
        return audioTracks;
    }

    private static List<Track> getVideoTracks(List<Movie> inMovies) throws IOException {
        List<Track> videoTracks = new LinkedList<Track>();
        for (Movie movie : inMovies) {
            for (Track track : movie.getTracks()) {
                if (track.getHandler().equals("vide")) {
                    videoTracks.add(track);
                }
            }
        }
        return videoTracks;
    }

    private static List<String> getGetNewVideosPaths(List<Track> trackList, List<String> videoUris) throws IOException {
        List<String> newVideosPaths = new ArrayList<String>();
        int counter = 1;
        for (Track track : trackList) {
            double duration = track.getDuration();
            double timescale = duration / track.getTrackMetaData().getTimescale();
            long[] sampleDurations = track.getSampleDurations();
            double moment;
            moment = getMoment(track, duration, timescale, sampleDurations);
            File cutVideo = new File(videoUris.get(counter - 1));
            String newName = cutVideos(cutVideo, moment, timescale, counter);
            newVideosPaths.add(videoFolderPath + newName);
            counter++;
        }
        return newVideosPaths;
    }

    private static double getMoment(Track track, double duration, double timescale, long[] sampleDurations) {
        double moment = 0;
        List<Sample> samples = track.getSamples();
        //long maxSample = 0;
        double sampleCounter = 0;
        /*
        for (int i = 0; i < samples.size(); i++) {
            sampleCounter += sampleDurations[i];
            if (samples.get(i).getSize() >= maxSample) {
                maxSample = samples.get(i).getSize();
                moment = sampleCounter * timescale / duration;
            }
        }*/
        int counter = 1;
        int highSampleDur = 100;
        ByteBuffer buffer = samples.get(0).asByteBuffer();
        byte[] b = new byte[buffer.remaining()];
        buffer.wrap(b);
        Log.i("BUFF", Integer.toString(ByteArrayTo.convertToInt(b)));
        for (int i = 0; i < samples.size(); i++) {
            sampleCounter += sampleDurations[i];
            if (samples.get(i).getSize() > 350 && i > 0) {
                if (samples.get(i).getSize() == samples.get(i - 1).getSize()) {
                    counter++;
                } else {
                    if (counter <= highSampleDur) {
                        highSampleDur = counter;
                        moment = sampleCounter * timescale / duration;
                        Log.i("SAMPLE", Long.toString(samples.get(i).getSize()));
                        Log.i("MOMENT", Double.toString(moment));
                        Log.i("COUNTER", Integer.toString(counter));
                    }
                    counter = 1;
                }
            }
        }
        Log.i("MOMENT", Double.toString(moment));
        return moment;
    }

    private static int getTotalSize(List<Sample> samples) {
        int totalSize = 0;
        for (int i = 0; i < samples.size(); i++) {
            totalSize += samples.get(i).getSize();
        }
        totalSize /= samples.size();
        return totalSize;
    }

    private static String cutVideos(File cutVideo, double moment, double timescale, int counter) throws IOException {
        String newName = "Cut" + counter + ".mp4";
        moment *= 1000;
        timescale *= 1000;
        if (counter == 1) {
            moment -= 150;
            Log.i("MOMENT", Double.toString(moment));
            TrimVideo.startTrim(cutVideo, videoFolderPath, 0, moment, newName);
        } else {
            Log.i("MOMENT", Double.toString(moment));
            TrimVideo.startTrim(cutVideo, videoFolderPath, moment, timescale, newName);
        }
        return newName;
    }

    private static File concat(List<Track> audioTracks, List<Track> videoTracks, List<String> filePaths) throws IOException, JCodecException {
        Movie result = new Movie();
        if (audioTracks.isEmpty() | videoTracks.isEmpty()) {
            return ResultsCodes.ERROR_CREATE_VIDEO;
        }
        result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        Container out = new DefaultMp4Builder().build(result);
        FileChannel fileChannel = new RandomAccessFile(String.format(videoFolderPath + "output.mp4"), "rw").getChannel();
        out.writeContainer(fileChannel);
        fileChannel.close();
        int isClear = WorkWithFiles.clear(filePaths);
        if (isClear == ResultsCodes.FILE_NOT_EXIST) {
            Log.v("ERROR", "File not exist");
        }
        return new File(videoFolderPath + "output.mp4");
    }
}
