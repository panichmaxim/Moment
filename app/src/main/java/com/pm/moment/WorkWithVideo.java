package com.pm.moment;

import android.os.Environment;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
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
            return output(audioTracks, videoTracks);
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
                } catch (Exception ignored) {
                }
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
        List<Movie> inMovies = new ArrayList<>();
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

    private static File output(List<Track> audioTrackList, List<Track> videoTrackList) throws IOException {
        Movie result = new Movie();
        int counter = 0;
        List<Track> audioTracks = new ArrayList<>();
        List<Track> videoTracks = new ArrayList<>();
        for (Track audioTrack : audioTrackList) {
            long moment = getMoment(audioTrack);
            long videoMoment = videoTrackList.get(counter).getSamples().size() * moment / audioTrack.getSamples().size();
            if (counter == 0) {
                audioTracks.add(new CroppedTrack(audioTrack, 0, moment));
                videoTracks.add(new CroppedTrack(videoTrackList.get(counter), 0, videoMoment));
            } else {
                audioTracks.add(new CroppedTrack(audioTrack, moment, audioTrack.getSamples().size()));
                videoTracks.add(new CroppedTrack(videoTrackList.get(counter), videoMoment, videoTrackList.get(counter).getSamples().size()));
            }
            counter++;
        }
        result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        return saveMovie(result, "output.mp4");
    }

    private static File saveMovie(Movie movie, String outputName) throws IOException {
        Container out = new DefaultMp4Builder().build(movie);
        Log.i("TAG", Long.toString(movie.getTimescale()));
        FileChannel fileChannel = new RandomAccessFile(String.format(videoFolderPath + outputName), "rw").getChannel();
        out.writeContainer(fileChannel);
        fileChannel.close();
        return new File(videoFolderPath + outputName);
    }

    private static long getMoment(Track track) {
        long moment = 0;
        List<Sample> samples = track.getSamples();
        int counter = 1;
        int highSampleDur = 100;
        long highestSound = 350;
        for (int i = 0; i < samples.size(); i++) {
            if (samples.get(i).getSize() > highestSound && i > 0) {
                if (samples.get(i).getSize() == samples.get(i - 1).getSize()) {
                    counter++;
                } else {
                    if (counter <= highSampleDur) {
                        highSampleDur = counter;
                        highestSound = samples.get(i).getSize();
                        moment = i;
                    }
                    counter = 1;
                }
            }
        }
        return moment;
    }
}
