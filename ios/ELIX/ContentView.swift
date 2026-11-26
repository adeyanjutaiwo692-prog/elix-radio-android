import SwiftUI
import AVKit
import PhotosUI

private let supabaseURL = URL(string: "https://sumqnvpvtqpbbbytmbvk.supabase.co")!
private let supabaseKey = "sb_publishable_-H-6950SAms-43NMmbM0xQ_rTz-lGl0"
private let gold = Color(red: 0.84, green: 0.69, blue: 0.40)

struct Ministry: Codable, Identifiable { let id: UUID; let name: String; var description: String?; var logo_url: String? }
struct MediaItem: Codable, Identifiable { let id: UUID; let title: String; var description: String?; var media_type: String; var external_url: String?; var file_path: String?; var thumbnail_url: String?; var preacher: String? }
@MainActor final class ELIXStore: ObservableObject {
 @Published var token: String? = UserDefaults.standard.string(forKey: "elix_token")
 @Published var email = ""; @Published var password = ""; @Published var displayName = ""
 @Published var ministries:[Ministry] = []; @Published var media:[MediaItem] = []; @Published var error = ""; @Published var busy = false
 var signedIn: Bool { token != nil }
 func request(_ path:String, method:String = "GET", body:Data? = nil, authenticated:Bool = true) async throws -> Data {
  var r=URLRequest(url:supabaseURL.appending(path:path)); r.httpMethod=method; r.httpBody=body
  r.setValue(supabaseKey,forHTTPHeaderField:"apikey"); r.setValue("application/json",forHTTPHeaderField:"Content-Type")
  r.setValue("Bearer "+((authenticated ? token : nil) ?? supabaseKey),forHTTPHeaderField:"Authorization")
  let (d,res)=try await URLSession.shared.data(for:r); guard let h=res as? HTTPURLResponse, (200...299).contains(h.statusCode) else { throw NSError(domain:String(data:d,encoding:.utf8) ?? "Request failed",code:1) }; return d
 }
 func auth(_ signup:Bool) async {
  busy=true; error=""
  defer{busy=false}
  do {
   let payload:[String:Any] = signup ? ["email":email,"password":password,"data":["full_name":displayName]] : ["email":email,"password":password]
   let data=try JSONSerialization.data(withJSONObject:payload)
   let d=try await request(signup ? "/auth/v1/signup" : "/auth/v1/token?grant_type=password",method:"POST",body:data,authenticated:false)
   let obj=try JSONSerialization.jsonObject(with:d) as? [String:Any]
   if let t=obj?["access_token"] as? String { token=t; UserDefaults.standard.set(t,forKey:"elix_token") }
   if token == nil { error="Check your email to confirm your account, then sign in." }
   await load()
  } catch { self.error=error.localizedDescription }
 }
 func load() async {
  do { let d=try await request("/rest/v1/ministries?select=id,name,description,logo_url&approved=eq.true&order=name"); ministries=try JSONDecoder().decode([Ministry].self,from:d) } catch { self.error=error.localizedDescription }
  do { let d=try await request("/rest/v1/media_items?select=id,title,description,media_type,external_url,file_path,thumbnail_url,preacher&status=eq.approved&order=published_at.desc&limit=40"); media=try JSONDecoder().decode([MediaItem].self,from:d) } catch { }
 }
 func signOut(){token=nil;UserDefaults.standard.removeObject(forKey:"elix_token")}
}

struct ContentView: View {
 @StateObject private var store=ELIXStore()
 var body: some View {
  Group { if store.signedIn { MainTabs().environmentObject(store).task{await store.load()} } else { AuthScreen().environmentObject(store) } }
  .tint(gold).background(Color(red:0.035,green:0.04,blue:0.07))
 }
}
struct AuthScreen: View {
 @EnvironmentObject var store:ELIXStore; @State private var signup=false
 var body: some View { ZStack { Color(red:0.035,green:0.04,blue:0.07).ignoresSafeArea(); VStack(spacing:18){
  Spacer(); Text("ELIX").font(.system(size:48,weight:.black,design:.rounded)).tracking(8).foregroundStyle(gold)
  Text("CHRISTIAN MEDIA NETWORK").font(.caption).tracking(3).foregroundStyle(.white.opacity(0.7))
  Text("Faith. Truth. Inspiration.").foregroundStyle(gold).padding(.bottom,22)
  if signup { TextField("Full name",text:$store.displayName).textContentType(.name).textFieldStyle(.roundedBorder) }
  TextField("Email address",text:$store.email).textInputAutocapitalization(.never).keyboardType(.emailAddress).textFieldStyle(.roundedBorder)
  SecureField("Password",text:$store.password).textFieldStyle(.roundedBorder)
  if !store.error.isEmpty { Text(store.error).font(.footnote).foregroundStyle(.red) }
  Button { Task{await store.auth(signup)} } label:{ if store.busy {ProgressView()} else {Text(signup ? "CREATE ACCOUNT":"SIGN IN").bold().frame(maxWidth:.infinity).padding()} }.background(gold).foregroundStyle(.black).clipShape(RoundedRectangle(cornerRadius:12))
  Button(signup ? "Already have an account? Sign in":"Create an account"){signup.toggle()}.foregroundStyle(gold)
  Spacer(); Text("A home for Christian ministries, messages and worship").font(.caption).foregroundStyle(.white.opacity(0.45)).multilineTextAlignment(.center)
 }.padding(26) } }
}
struct MainTabs: View {
 @EnvironmentObject var store:ELIXStore
 var body: some View { TabView {
  HomeScreen().tabItem{Label("Home",systemImage:"house.fill")}
  DiscoverScreen().tabItem{Label("Discover",systemImage:"play.rectangle.fill")}
  ExploreScreen().tabItem{Label("Explore",systemImage:"safari.fill")}
  AccountScreen().tabItem{Label("Account",systemImage:"person.crop.circle")}
 }.toolbarBackground(Color.black,for:.tabBar).toolbarBackground(.visible,for:.tabBar) }
}
struct HomeScreen: View {
 @EnvironmentObject var store:ELIXStore
 var body: some View { NavigationStack { ScrollView { VStack(alignment:.leading,spacing:18){
  brandHeader
  Text("MINISTRIES & PAGES").font(.caption).tracking(2).foregroundStyle(gold)
  if store.ministries.isEmpty { empty("Ministry pages will appear here.") }
  ForEach(store.ministries){m in HStack(spacing:14){Image(systemName:"building.2.crop.circle.fill").font(.largeTitle).foregroundStyle(gold); VStack(alignment:.leading){Text(m.name).font(.headline);Text(m.description ?? "Christian ministry").font(.caption).foregroundStyle(.secondary)};Spacer();Image(systemName:"chevron.right").foregroundStyle(.secondary)}.padding().background(Color.white.opacity(0.06)).clipShape(RoundedRectangle(cornerRadius:14)) }
  Text("LIVE & FEATURED").font(.caption).tracking(2).foregroundStyle(gold)
  NavigationLink(destination:RadioScreen()){Label("ELIX Live Radio",systemImage:"dot.radiowaves.left.and.right").font(.headline).padding().frame(maxWidth:.infinity).background(gold.opacity(0.16)).clipShape(RoundedRectangle(cornerRadius:14))}
 }.padding() }.background(Color(red:0.035,green:0.04,blue:0.07)).navigationBarHidden(true) } }
}
struct DiscoverScreen: View {
 @EnvironmentObject var store:ELIXStore
 var body: some View { ZStack { Color.black.ignoresSafeArea()
  if store.media.isEmpty { empty("No published content yet. Check back soon.") }
  else { TabView { ForEach(store.media){item in FeedCard(item:item)} }.tabViewStyle(.page(indexDisplayMode:.never)).rotationEffect(.degrees(-90)).frame(width:UIScreen.main.bounds.height,height:UIScreen.main.bounds.width).offset(x:(UIScreen.main.bounds.width-UIScreen.main.bounds.height)/2,y:(UIScreen.main.bounds.height-UIScreen.main.bounds.width)/2) }
 }.task{await store.load()} }
}
struct FeedCard: View {
 let item:MediaItem
 var body: some View { ZStack(alignment:.bottomLeading) {
  if let s=item.external_url, let url=URL(string:s), item.media_type=="video" || item.media_type=="audio" { VideoPlayer(player:AVPlayer(url:url)).ignoresSafeArea() } else { LinearGradient(colors:[.black,.indigo.opacity(0.7),.black],startPoint:.topLeading,endPoint:.bottomTrailing); Image(systemName:item.media_type=="audio" ? "waveform":"play.rectangle.fill").font(.system(size:70)).foregroundStyle(gold.opacity(0.8)).frame(maxWidth:.infinity,maxHeight:.infinity) }
  VStack(alignment:.leading,spacing:8){Text(item.title).font(.title2.bold());Text(item.preacher ?? "ELIX Creator").foregroundStyle(.white.opacity(0.8));Text(item.description ?? "").font(.subheadline).lineLimit(3)}.padding(26).frame(maxWidth:.infinity,alignment:.leading).background(LinearGradient(colors:[.clear,.black.opacity(0.85)],startPoint:.top,endPoint:.bottom))
 }.frame(width:UIScreen.main.bounds.height,height:UIScreen.main.bounds.width) }
}
struct ExploreScreen: View {
 var body: some View { NavigationStack { List { NavigationLink("Live Radio",destination:RadioScreen()); NavigationLink("Live Video",destination:RadioScreen()); Label("Sermons & Teaching",systemImage:"book.closed"); Label("Worship",systemImage:"music.note"); Label("Programmes",systemImage:"calendar") }.scrollContentBackground(.hidden).background(Color.black).navigationTitle("Explore ELIX") } }
}
struct RadioScreen: View {
 var body: some View { VStack(spacing:20){Image(systemName:"dot.radiowaves.left.and.right").font(.system(size:64)).foregroundStyle(gold);Text("ELIX LIVE RADIO").font(.title2.bold());Text("Faith. Truth. Inspiration.").foregroundStyle(.secondary);Text("Radio stream connection will be configured here.").multilineTextAlignment(.center)}.frame(maxWidth:.infinity,maxHeight:.infinity).background(Color.black).navigationTitle("Live Radio") }
}
struct AccountScreen: View {
 @EnvironmentObject var store:ELIXStore
 var body: some View { NavigationStack { VStack(spacing:18){Image(systemName:"person.crop.circle.fill").font(.system(size:70)).foregroundStyle(gold);Text("Your ELIX Account").font(.title2.bold());Text(store.email).foregroundStyle(.secondary);NavigationLink("Discover Creators"){ CreatorDirectoryView().environmentObject(store) }.foregroundStyle(gold).padding(.vertical,8)
 Button("Sign Out"){store.signOut()}.buttonStyle(.borderedProminent).tint(gold).foregroundStyle(.black)}.frame(maxWidth:.infinity,maxHeight:.infinity).background(Color.black).navigationTitle("Account") } }
}
private var gold:Color { Color(red:0.84,green:0.69,blue:0.40) }
private func empty(_ message:String)->some View { VStack(spacing:12){Image(systemName:"sparkles").font(.largeTitle).foregroundStyle(gold);Text(message).foregroundStyle(.secondary).multilineTextAlignment(.center)}.frame(maxWidth:.infinity,maxHeight:.infinity).padding() }
private var brandHeader:some View { VStack(alignment:.leading,spacing:6){Text("ELIX").font(.system(size:32,weight:.black)).tracking(5).foregroundStyle(gold);Text("CHRISTIAN MEDIA NETWORK").font(.caption2).tracking(2);Text("Faith. Truth. Inspiration.").font(.subheadline).foregroundStyle(.secondary)}.padding(.vertical,18) }


struct CreatorProfile: Codable, Identifiable {
 let id: UUID
 var display_name: String?
}
struct CreatorMedia: Codable, Identifiable {
 let id: UUID
 let title: String
 var description: String?
 var media_type: String
 var file_path: String?
}
struct CreatorDirectoryView: View {
 @EnvironmentObject var store: ELIXStore
 @State private var creators:[CreatorProfile] = []
 @State private var selected:CreatorProfile?
 @State private var media:[CreatorMedia] = []
 @State private var following = false
 @State private var followers = 0
 @State private var error = ""
 @State private var busy = false
 private var myId:String? {
  guard let token=store.token else{return nil}
  let parts=token.split(separator:".")
  guard parts.count>1, let data=Data(base64Encoded:String(parts[1]).padding(toLength:((String(parts[1]).count+3)/4)*4,withPad:"=",startingAt:0)), let obj=try? JSONSerialization.jsonObject(with:data) as? [String:Any] else{return nil}
  return obj["sub"] as? String
 }
 var body:some View {
  List {
   if let creator=selected {
    Section {
     HStack { Text(creator.display_name ?? "ELIX Creator").font(.title3.bold()); Spacer(); Text("\(followers) followers").foregroundStyle(gold) }
     if creator.id.uuidString.lowercased() != myId?.lowercased() {
      Button(following ? "Following · Unfollow" : "＋ Follow") { Task { await toggleFollow(creator) } }.disabled(busy)
     }
    }
    Section("Published content") {
     if media.isEmpty { Text("No published content yet.").foregroundStyle(.secondary) }
     ForEach(media) { item in VStack(alignment:.leading,spacing:5) { Text(item.title).font(.headline); Text(item.media_type.uppercased()).font(.caption).foregroundStyle(.secondary) } }
    }
    Button("Back to creators") { selected=nil; media=[] }
   } else {
    Section("ELIX creator community") {
     if creators.isEmpty { Text(error.isEmpty ? "Loading creators…" : error).foregroundStyle(.secondary) }
     ForEach(creators.filter { $0.id.uuidString.lowercased() != myId?.lowercased() }) { creator in
      Button { Task { await openCreator(creator) } } label: {
       HStack { Image(systemName:"person.crop.circle.fill").font(.title2).foregroundStyle(gold); Text(creator.display_name ?? "ELIX Creator").foregroundStyle(.primary); Spacer(); Image(systemName:"chevron.right").foregroundStyle(.secondary) }
      }
     }
    }
   }
  }
  .navigationTitle(selected == nil ? "Creators" : "Creator profile")
  .task { await loadCreators() }
 }
 private func loadCreators() async {
  do {
   let data=try await store.request("/rest/v1/profiles?select=id,display_name&order=created_at.desc&limit=200")
   creators=try JSONDecoder().decode([CreatorProfile].self,from:data)
  } catch { self.error=error.localizedDescription }
 }
 private func openCreator(_ creator:CreatorProfile) async {
  selected=creator
  do {
   let id=creator.id.uuidString.lowercased()
   let data=try await store.request("/rest/v1/media_items?select=id,title,description,media_type,file_path&uploaded_by=eq.\(id)&status=eq.approved&order=created_at.desc&limit=100")
   media=try JSONDecoder().decode([CreatorMedia].self,from:data)
   let counts=try await store.request("/rest/v1/creator_follower_counts?select=follower_count&creator_id=eq.\(id)")
   if let row=(try JSONSerialization.jsonObject(with:counts) as? [[String:Any]])?.first { followers=(row["follower_count"] as? Int) ?? 0 }
   if let me=myId, me.lowercased() != id {
    let mine=try await store.request("/rest/v1/creator_follows?select=creator_id&follower_id=eq.\(me)&creator_id=eq.\(id)")
    following = ((try JSONSerialization.jsonObject(with:mine) as? [[String:Any]])?.isEmpty == false)
   }
  } catch { self.error=error.localizedDescription }
 }
 private func toggleFollow(_ creator:CreatorProfile) async {
  guard let me=myId else { error="Please sign in again to follow creators."; return }
  busy=true; defer{busy=false}
  do {
   let id=creator.id.uuidString.lowercased()
   let path="/rest/v1/creator_follows?follower_id=eq.\(me)&creator_id=eq.\(id)"
   if following { _ = try await store.request(path,method:"DELETE") }
   else {
    let body=try JSONSerialization.data(withJSONObject:["follower_id":me,"creator_id":id])
    _ = try await store.request("/rest/v1/creator_follows",method:"POST",body:body)
   }
   following.toggle()
   followers += following ? 1 : -1
  } catch { self.error=error.localizedDescription }
 }
}
