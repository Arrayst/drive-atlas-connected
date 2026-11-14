package com.driveatlas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
internal data class Guide(val seasons:List<String>,val moods:List<String>,val reason:String)
internal fun guide(r:Route):Guide=when(r.id){
 "coast"->Guide(listOf("봄","여름","가을"),listOf("탁 트인"),"강릉 금진항에서 정동진 방향으로. 헌화로의 바다 풍경을 함께 보고 싶은 날에.")
 "forest"->Guide(listOf("봄","가을"),listOf("잔잔한","초록 충전"),"담양의 관방제림과 메타길 주변을 잇는 짧은 나들이. 메타세쿼이아길은 차가 아니라 주차 후 걸어요.")
 else->Guide(listOf("여름","가을","겨울"),listOf("잔잔한","탁 트인"),"의암댐에서 춘천댐 방향, 의암호 서쪽 길. 창밖의 물빛을 따라가는 코스예요.")
}
internal fun filteredRoutes(mode:String,season:String,mood:String,region:String,near:Boolean):List<Route> = Catalog.routes.filter{r->
 (region=="전체"||regionFor(r)==region)&&(mode!="season"||season in guide(r).seasons)&&(mood=="전체"||mood in guide(r).moods)
}
@Composable internal fun GuideHome(snapshot:Snapshot,onCategory:(String)->Unit,onTrip:()->Unit,onAtlas:()->Unit,onRoute:(Route)->Unit,filters:GuideFilters) =
 DesignHome(snapshot,onCategory,onTrip,onAtlas,onRoute,filters)
@Composable internal fun ChoiceRow(label:String,values:List<String>,selected:String,onSelect:(String)->Unit){
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Text(label,Modifier.width(100.dp),fontSize=18.sp,color=Muted)
  values.forEach{v->FilterChip(selected=v==selected,onClick={onSelect(v)},label={Text(v,fontSize=20.sp)},modifier=Modifier.heightIn(min=76.dp).widthIn(min=76.dp),shape=RoundedCornerShape(10.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=AtlasSoft,selectedLabelColor=AtlasGreen))}
 }
}
internal class GuideFilters {
 val homeMode = mutableStateOf("recommended")
 val season = mutableStateOf("가을")
 val mood = mutableStateOf("전체")
 val region = mutableStateOf("전체")
 val near = mutableStateOf(false)
 val options = mutableStateOf(false)
}
@Composable internal fun CurationPage(mode:String,filters:GuideFilters,awards:List<Award>,onRoute:(Route)->Unit,onBack:()->Unit) =
 DesignCuration(mode,filters,awards,onRoute,onBack)
@Composable internal fun ArchiveDialog(onDismiss:()->Unit,onArchive:(VisitStatus)->Unit){
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Surface(Modifier.widthIn(max=730.dp).padding(24.dp),color=Paper,shape=Corner){
   Column(Modifier.padding(32.dp)){
    Eyebrow("KEEP THE MEMORY");Text("기억은 남기고, 우표는 기다려요",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(vertical=18.dp))
    Text("완주 근거가 부족하면 여행 기록만 남깁니다. 빠진 구간을 채우러 되돌아갈 필요는 없어요. 안전이 먼저입니다.",fontSize=18.sp,lineHeight=28.sp,color=Muted);Spacer(Modifier.height(24.dp))
    PrimaryButton("미확인 기록 남기기",{onArchive(VisitStatus.UNCONFIRMED)},Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));SecondaryButton("중단 기록 남기기",{onArchive(VisitStatus.INTERRUPTED)},Modifier.fillMaxWidth())
    TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End).heightIn(min=56.dp)){Text("계속 체험하기",fontSize=18.sp)}
   }
  }
 }
}

@Composable internal fun RecordsPage(snapshot:Snapshot,onRoute:(Route)->Unit) = DesignRecords(snapshot,onRoute)
