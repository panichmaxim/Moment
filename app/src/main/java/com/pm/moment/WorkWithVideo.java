package com.pm.moment;

import android.os.Environment;
import android.util.Log;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
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
        int counter = 0;
        for (Track track : trackList) {
            counter++;
            long moment = getMoment(track);
            File cutVideo = new File(videoUris.get(counter - 1));
            String newName = cutVideos(cutVideo, moment, track.getTrackMetaData().getTimescale(), counter);
            newVideosPaths.add(videoFolderPath + newName);
        }
        return newVideosPaths;
    }

    private static long getMoment(Track track) {
        long moment = 0;
        List<Sample> samples = track.getSamples();
        long duration = track.getDuration();
        long timescale = duration / track.getTrackMetaData().getTimescale();
        long[] sampleDurations = track.getSampleDurations();
        long maxSample = 0;
        long sampleCounter = 0;
        for (int i = 0; i < samples.size(); i++) {
            sampleCounter += sampleDurations[0];
            if (samples.get(i).getSize() >= maxSample) {
                maxSample = samples.get(i).getSize();
                moment = sampleCounter * timescale / duration;
            }
        }
        return moment;
    }

    private static String cutVideos(File cutVideo, long moment, long timescale, int counter) throws IOException {
        String newName = "File" + counter + ".mp4";
        if (counter == 1) {
            TrimVideo.startTrim(cutVideo, videoFolderPath, 0, moment * 1000 + 1000, newName);
        } else {
            TrimVideo.startTrim(cutVideo, videoFolderPath, moment * 1000 + 2000, timescale * 1000, newName);
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
