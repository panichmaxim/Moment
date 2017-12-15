package com.pm.moment;

import android.os.Environment;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
        convertToMp4(videoUris);
        List<Movie> movies = initVideos(videoUris);
        if (movies != ResultsCodes.INIT_VIDEO_NOT_EXIST) {
            List<Track> audioTracks = getAudioTracks(movies);
            List<Track> videoTracks = getVideoTracks(movies);
            List<String> newVideosPaths = getGetNewVideosPaths(audioTracks, videoTracks, videoUris);
            movies = initVideos(newVideosPaths);
            audioTracks = getAudioTracks(movies);
            videoTracks = getVideoTracks(movies);
            return concat(audioTracks, videoTracks, newVideosPaths);
        }
        return ResultsCodes.ERROR_CREATE_VIDEO;
    }

    private static void convertToMp4(List<String> videoUris) {
        int k = 0;
        for (String videoUri : videoUris) {
            if (videoUri.contains(".3gp")) {
                try {
                    String output = videoFolderPath + "converted_video_" + k + ".mp4";
                    convertVideo(videoUri, output);
                    videoUris.set(k, output);
                    k++;
                } catch (Exception ignored) {}
            }
        }
    }

    private static void convertVideo(String input, String output) throws IOException, InterruptedException, FFmpegNotSupportedException {
        FFmpeg ffmpeg = FFmpeg.getInstance(App.context);
        ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
            @Override
            public void onStart() {
            }

            @Override
            public void onFailure() {
            }

            @Override
            public void onSuccess() {
            }

            @Override
            public void onFinish() {
            }
        });
        String args = ffmpeg + " -i " + input + " ffmpeg encoding paramaters... " + "-f mp4" + output;
        Runtime r = Runtime.getRuntime();
        Process p = r.exec("/system/bin/sh" + "-c" + args);
        p.waitFor();
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

    private static List<String> getGetNewVideosPaths(List<Track> audioTrackList, List<Track> videoTrackList, List<String> videoUris) throws IOException {
        List<String> newVideosPaths = new ArrayList<String>();
        int counter = 1;
        for (Track track : audioTrackList) {
            //double duration = track.getDuration(); // длительность в байтах
            double timescale = (double) track.getDuration() / (double) track.getTrackMetaData().getTimescale(); // в секундах длительность
            double moment;
            moment = getMoment(track);
            File cutVideo = new File(videoUris.get(counter - 1));
            //String newName = cutVideos(cutVideo, moment, timescale, counter);
            String newName = "Cut" + counter + ".mp4";
            long videoMoment = videoTrackList.get(counter - 1).getSamples().size() * (long) moment / track.getSamples().size();
            Log.i("MOMENT", Long.toString((long)moment));
            Log.i("VIDEO MOMENT", Long.toString(videoMoment));
            if (counter == 1) {
                newTrim(cutVideo, (long) moment, track.getSamples().size(), videoMoment,
                        videoTrackList.get(counter - 1).getSamples().size(), newName);
            } else {
                newTrim(cutVideo, 0, (long) moment, 0, videoMoment, newName);
            }
            newVideosPaths.add(videoFolderPath + newName);
            counter++;
        }
        return newVideosPaths;
    }

    private static double getMoment(Track track) {
        double moment = 0;
        List<Sample> samples = track.getSamples();
        long[] sampleDurations = track.getSampleDurations();
        double sampleCounter = 0;
        int counter = 1;
        int highSampleDur = 100;
        long highestSound = 350;
        for (int i = 0; i < samples.size(); i++) {
            sampleCounter += (double) sampleDurations[i];
            if (samples.get(i).getSize() > highestSound && i > 0) {
                if (samples.get(i).getSize() == samples.get(i - 1).getSize()) {
                    counter++;
                } else {
                    if (counter <= highSampleDur) {
                        highSampleDur = counter;
                        highestSound = samples.get(i).getSize();
                        //moment = sampleCounter / (double) track.getTrackMetaData().getTimescale();
                        moment = i;
                    }
                    counter = 1;
                }
            }
        }
        return moment;
    }

    private static String cutVideos(File cutVideo, double moment, double timescale, int counter) throws IOException {
        String newName = "Cut" + counter + ".mp4";
        moment *= 1000;
        timescale *= 1000;
        if (counter == 1) {
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

    private static void newTrim(File cutVideo, long startSample, long endSample, long startVideoSample,
                                long endVideoSample, String newName) throws IOException {
        final String fileName = newName;
        final String filePath = videoFolderPath + fileName;

        File file = new File(filePath);
        file.getParentFile().mkdirs();
        newTrim1(cutVideo, file, startSample, endSample, startVideoSample, endVideoSample);
    }

    private static void newTrim1(File cutVideo, File dst, long startSample, long endSample,
                                 long startVideoSample, long endVideoSample) throws IOException {
        Movie movie = MovieCreator.build(new FileDataSourceViaHeapImpl(cutVideo.getAbsolutePath()));
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());
        for (Track track : tracks) {
            if (track.getHandler().equals("soun")) {
                movie.addTrack(new AppendTrack(new CroppedTrack(track, startSample, endSample)));
            } else if (track.getHandler().equals("vide")) {
                movie.addTrack(new AppendTrack(new CroppedTrack(track, startVideoSample, endVideoSample)));
            }
        }
        dst.getParentFile().mkdirs();
        if (!dst.exists()) {
            dst.createNewFile();
        }
        Container out = new DefaultMp4Builder().build(movie);
        FileOutputStream fos = new FileOutputStream(dst);
        FileChannel fc = fos.getChannel();
        out.writeContainer(fc);
        fc.close();
        fos.close();
    }
}
