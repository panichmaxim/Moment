package com.pm.moment;

import android.os.Environment;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.TrackMetaData;
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

    private String videoFolderPath = Environment.getExternalStorageDirectory() + "/Collapse/";

    public File init(List<String> videoUris) throws IOException, JCodecException {
        List<Movie> movies = initVideos(videoUris);
        List<Track> audioTracks = getAudioTracks(movies);
        List<String> newVideosPaths = getMomentVideos(audioTracks, videoUris);
        movies = initVideos(newVideosPaths);
        audioTracks = getAudioTracks(movies);
        List<Track> videoTracks = getVideoTracks(movies);
        return concat(audioTracks, videoTracks, newVideosPaths);
    }

    private List<Movie> initVideos(List<String> videoUris) throws IOException, JCodecException {
        List<Movie> inMovies = new ArrayList<Movie>();
        for (String videoUri : videoUris) {
            inMovies.add(MovieCreator.build(videoUri));
        }
        return inMovies;
    }

    private List<Track> getAudioTracks(List<Movie> inMovies) throws IOException {
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

    private List<Track> getVideoTracks(List<Movie> inMovies) throws IOException {
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

    private List<String> getMomentVideos(List<Track> trackList, List<String> videoUris) throws IOException {
        checkDefaultDir();
        List<String> newVideosPaths = new ArrayList<String>();
        int counter = 0;
        for (Track track : trackList) {
            counter++;
            long moment = 0;
            List<Sample> samples = track.getSamples();
            long duration = track.getDuration();
            TrackMetaData trackMetaData = track.getTrackMetaData();
            long timescale = duration / trackMetaData.getTimescale();
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
            File cutVideo = new File(videoUris.get(counter - 1));
            String newName = "File" + counter + ".mp4";
            if (counter == 1) {
                TrimVideo.startTrim(cutVideo, videoFolderPath, 0, moment * 1000 + 1000, newName);
            } else {
                TrimVideo.startTrim(cutVideo, videoFolderPath, moment * 1000 + 2000, timescale * 1000, newName);
            }
            newVideosPaths.add(videoFolderPath + newName);
        }
        return newVideosPaths;
    }

    private File concat(List<Track> audioTracks, List<Track> videoTracks, List<String> filePaths) throws IOException, JCodecException {
        Movie result = new Movie();
        if (!audioTracks.isEmpty()) {
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (!videoTracks.isEmpty()) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }
        Container out = new DefaultMp4Builder().build(result);
        FileChannel fileChannel = new RandomAccessFile(String.format(videoFolderPath + "output.mp4"), "rw").getChannel();
        out.writeContainer(fileChannel);
        fileChannel.close();
        clear(filePaths);
        return new File(videoFolderPath + "output.mp4");
    }

    private void checkDefaultDir() {
        File defaultDir = new File(videoFolderPath);
        if (!defaultDir.exists()) {
            defaultDir.mkdir();
        }
    }

    private void clear(List<String> filePaths) {
        for (int i = 0; i < filePaths.size(); i++) {
            File file = new File(filePaths.get(i));
            file.delete();
        }
    }
}
