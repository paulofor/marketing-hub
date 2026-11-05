package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.AssetType;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: comprovar duração final, áudio e legendas na montagem real sem render pago. */
class RunwayMontageDurationTest {
  @TempDir Path temp;

  /** Monta 32 segundos e corta somente excedente explícito de 16 para 15, preservando streams. */
  @Test void preservesFinalDurationAudioAndSubtitles() throws Exception {
    for (int target : new int[]{32,15}) {
      var model=com.marketinghub.videocontract.v1.VideoCapabilities.model(target==32?"gen4.5":"veo3.1_fast").orElseThrow();
      var plan=model.plan(target);var clips=new ArrayList<ProviderFile>();int index=0;
      for (int seconds : plan.clipSeconds()) {
        Path caption=temp.resolve("caption-"+index+".srt");Files.writeString(caption,"1\n00:00:00,000 --> 00:00:01,000\nTeste sintético local\n");
        Path file=temp.resolve(target+"-"+(index++)+".mp4");
        var process=new ProcessBuilder("ffmpeg","-v","error","-y","-f","lavfi","-i","color=c=blue:s=160x284:r=10:d="+seconds,
            "-f","lavfi","-i","sine=frequency=440:sample_rate=22050:duration="+seconds,"-i",caption.toString(),
            "-map","0:v","-map","1:a","-map","2:s","-c:v","libx264","-threads","1","-preset","ultrafast","-pix_fmt","yuv420p",
            "-c:a","aac","-c:s","mov_text","-t",String.valueOf(seconds),file.toString()).inheritIO().start();
        assertThat(process.waitFor()).isZero();
        clips.add(new ProviderFile(file.getFileName().toString(),MediaType.valueOf("video/mp4"),AssetType.VIDEO,ProviderAssetRole.VIDEO,Files.readAllBytes(file)));
      }
      var job=mock(SalesVideoJob.class);when(job.id()).thenReturn(880001L);when(job.metadataJson()).thenReturn("{\"targetDurationSeconds\":"+target+"}");
      var provider=new RunwayVideoProvider(new VideoManagementProperties(),new ObjectMapper(),WebClient.builder());
      var assemble=RunwayVideoProvider.class.getDeclaredMethod("assembleScenes",SalesVideoJob.class,java.util.List.class);assemble.setAccessible(true);
      var output=(ProviderFile) assemble.invoke(provider,job,clips);Path movie=temp.resolve("final-"+target+".mp4");Files.write(movie,output.content());
      var probe=new ProcessBuilder("ffprobe","-v","error","-show_streams","-show_format","-of","json",movie.toString()).start();
      var metadata=new ObjectMapper().readTree(probe.getInputStream());assertThat(probe.waitFor()).isZero();
      assertThat(metadata.path("format").path("duration").asDouble()).isCloseTo(target,within(0.15));
      var types=new java.util.HashSet<String>();metadata.path("streams").forEach(stream -> types.add(stream.path("codec_type").asText()));
      assertThat(types).contains("video","audio","subtitle");
      System.out.println("PASS montagem sintética: final="+target+"s cobrados="+plan.generatedSeconds()+"s; áudio e legenda preservados");
    }
  }
}
